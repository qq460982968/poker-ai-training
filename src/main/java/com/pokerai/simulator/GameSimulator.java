package com.pokerai.simulator;

import com.pokerai.ai.AIPlayer;
import com.pokerai.model.Card;
import com.pokerai.model.Deck;
import com.pokerai.model.Player;
import com.pokerai.rules.GameRules;
import java.util.*;

/**
 * 游戏模拟器 - 模拟4人对战
 */
public class GameSimulator {
    
    private static final String[] DAO_NAMES = {"头道", "中道", "尾道"};
    private static final int[] DAO_POINTS = {12, -2, -4, -6};
    
    private Deck deck;
    private List<Player> players;
    private AIPlayer aiPlayer; // AI玩家（第一个玩家）
    
    public GameSimulator() {
        this.deck = new Deck();
        this.players = new ArrayList<>();
        
        // 创建4个玩家，第一个是AI玩家
        this.aiPlayer = new AIPlayer(0, "AI玩家");
        players.add(aiPlayer);
        players.add(new Player(1, "玩家2"));
        players.add(new Player(2, "玩家3"));
        players.add(new Player(3, "玩家4"));
    }
    
    /**
     * 执行一局游戏
     * @return AI玩家是否获胜
     */
    public boolean playOneGame() {
        return simulateGame(false).aiWon;
    }

    /**
     * 执行一局并输出详细记录
     */
    public GameResult playOneGameWithDetails() {
        GameOutcome outcome = simulateGame(true);
        if (outcome.result != null) {
            printDetailedResult(outcome.result);
        }
        return outcome.result;
    }

    private GameOutcome simulateGame(boolean captureDetails) {
        deck.reset();

        for (Player player : players) {
            List<Card> cards = deck.deal(9);
            player.receiveCards(cards);
        }

        LinkedHashMap<Player, List<List<Card>>> playerGroups = new LinkedHashMap<>();

        List<List<Card>> aiGroups = GameRules.sortGroupsAscending(aiPlayer.decideCardGroups());
        aiPlayer.setCardGroups(aiGroups);
        playerGroups.put(aiPlayer, aiGroups);

        for (int i = 1; i < players.size(); i++) {
            Player player = players.get(i);
            List<List<Card>> groups = GameRules.sortGroupsAscending(generateSimpleStrategy(player.getHand()));
            player.setCardGroups(groups);
            playerGroups.put(player, groups);
        }

        GameResult result = buildGameResult(playerGroups);

        boolean aiWon = result.getWinners().contains(aiPlayer);
        int aiPoints = result.getTotalPoints().getOrDefault(aiPlayer, 0);

        aiPlayer.learn(aiGroups, aiWon, aiPoints);
        if (aiWon) {
            aiPlayer.addWin();
        } else {
            aiPlayer.addGame();
        }

        for (Player player : players) {
            if (player == aiPlayer) {
                continue;
            }
            if (result.getWinners().contains(player)) {
                player.addWin();
            } else {
                player.addGame();
            }
        }

        return new GameOutcome(aiWon, captureDetails ? result : null);
    }

    private GameResult buildGameResult(LinkedHashMap<Player, List<List<Card>>> playerGroups) {
        List<GameResult.DaoResult> daoResults = new ArrayList<>();
        LinkedHashMap<Player, Integer> totalPoints = new LinkedHashMap<>();
        LinkedHashMap<Player, List<Integer>> daoPoints = new LinkedHashMap<>();
        LinkedHashMap<Player, List<Card>> initialHands = new LinkedHashMap<>();

        // 记录原始牌（9张）
        for (Player player : playerGroups.keySet()) {
            initialHands.put(player, new ArrayList<>(player.getHand()));
        }

        for (int daoIndex = 0; daoIndex < DAO_NAMES.length; daoIndex++) {
            List<GameResult.PlayerDaoScore> rankings = new ArrayList<>();

            for (Map.Entry<Player, List<List<Card>>> entry : playerGroups.entrySet()) {
                Player player = entry.getKey();
                List<Card> group = entry.getValue().get(daoIndex);
                GameRules.HandResult handResult = GameRules.evaluateHand(group);
                rankings.add(new GameResult.PlayerDaoScore(player, group, handResult));
            }

            rankings.sort((a, b) -> {
                int compare = GameRules.compareHands(b.getHandResult(), a.getHandResult());
                if (compare != 0) {
                    return compare;
                }
                return Integer.compare(a.getPlayer().getId(), b.getPlayer().getId());
            });

            for (int rankIndex = 0; rankIndex < rankings.size(); rankIndex++) {
                GameResult.PlayerDaoScore score = rankings.get(rankIndex);
                int points = DAO_POINTS[Math.min(rankIndex, DAO_POINTS.length - 1)];
                score.setPoints(points);

                totalPoints.merge(score.getPlayer(), points, Integer::sum);

                daoPoints.computeIfAbsent(
                    score.getPlayer(),
                    player -> new ArrayList<>(Arrays.asList(0, 0, 0))
                ).set(daoIndex, points);
            }

            daoResults.add(new GameResult.DaoResult(DAO_NAMES[daoIndex], rankings));
        }

        int maxPoints = Integer.MIN_VALUE;
        List<Player> winners = new ArrayList<>();
        for (Map.Entry<Player, Integer> entry : totalPoints.entrySet()) {
            int value = entry.getValue();
            if (value > maxPoints) {
                winners.clear();
                winners.add(entry.getKey());
                maxPoints = value;
            } else if (value == maxPoints) {
                winners.add(entry.getKey());
            }
        }

        return new GameResult(new ArrayList<>(playerGroups.keySet()), daoResults, totalPoints, daoPoints, winners, initialHands);
    }

    private void printDetailedResult(GameResult result) {
        System.out.println("========================================");
        System.out.println("单局牌局结果");
        System.out.println("原始牌：");
        for (Player player : result.getPlayerOrder()) {
            List<Card> hand = result.getInitialHands().get(player);
            System.out.printf("  %s: %s%n", player.getName(), formatCards(hand));
        }
        System.out.println();
        for (GameResult.DaoResult daoResult : result.getDaoResults()) {
            System.out.println(daoResult.getName() + "：");
            for (GameResult.PlayerDaoScore entry : daoResult.getRankings()) {
                String original = formatCards(entry.getOriginalCards());
                String resolved = formatCards(entry.getHandResult().getCards());
                System.out.printf(
                    "  %s | 原始牌: %s | 最佳牌型: %s | 最优组合: %s | 本道得分: %+d%n",
                    entry.getPlayer().getName(),
                    original,
                    entry.getHandResult().getHandType().getName(),
                    resolved,
                    entry.getPoints()
                );
            }
            System.out.println();
        }

        System.out.println("得分汇总：");
        for (Player player : result.getPlayerOrder()) {
            List<Integer> breakdown = result.getDaoPoints().get(player);
            int total = result.getTotalPoints().getOrDefault(player, 0);
            System.out.printf(
                "  %s -> 头道:%+d 中道:%+d 尾道:%+d | 总分:%+d%n",
                player.getName(),
                breakdown.get(0),
                breakdown.get(1),
                breakdown.get(2),
                total
            );
        }

        List<String> winnerNames = new ArrayList<>();
        for (Player player : result.getWinners()) {
            winnerNames.add(player.getName());
        }
        System.out.println("获胜玩家: " + (winnerNames.isEmpty() ? "无" : String.join("、", winnerNames)));
        System.out.println("========================================");
    }

    private String formatCards(List<Card> cards) {
        List<String> labels = new ArrayList<>();
        for (Card card : cards) {
            labels.add(card.toString());
        }
        return String.join(" ", labels);
    }

    private static class GameOutcome {
        private final boolean aiWon;
        private final GameResult result;

        private GameOutcome(boolean aiWon, GameResult result) {
            this.aiWon = aiWon;
            this.result = result;
        }
    }
    
    /**
     * 为普通玩家生成简单策略（贪心算法）
     */
    private List<List<Card>> generateSimpleStrategy(List<Card> hand) {
        List<Card> remaining = new ArrayList<>(hand);
        List<List<Card>> groups = new ArrayList<>();
        
        // 贪心策略：每次选择能组成最好牌型的3张牌
        for (int i = 0; i < 3; i++) {
            List<Card> bestGroup = findBestGroup(remaining);
            groups.add(bestGroup);
            remaining.removeAll(bestGroup);
        }
        
        return groups;
    }
    
    /**
     * 从剩余牌中找到能组成最好牌型的3张牌
     */
    private List<Card> findBestGroup(List<Card> cards) {
        if (cards.size() < 3) {
            return new ArrayList<>(cards);
        }
        
        List<Card> bestGroup = null;
        int bestScore = -1;
        
        // 尝试所有3张牌的组合
        for (int i = 0; i < cards.size(); i++) {
            for (int j = i + 1; j < cards.size(); j++) {
                for (int k = j + 1; k < cards.size(); k++) {
                    List<Card> group = Arrays.asList(cards.get(i), cards.get(j), cards.get(k));
                    GameRules.HandResult result = GameRules.evaluateHand(group);
                    if (result.getScore() > bestScore) {
                        bestScore = result.getScore();
                        bestGroup = new ArrayList<>(group);
                    }
                }
            }
        }
        
        return bestGroup != null ? bestGroup : cards.subList(0, Math.min(3, cards.size()));
    }
    
    /**
     * 获取AI玩家
     */
    public AIPlayer getAIPlayer() {
        return aiPlayer;
    }
    
    /**
     * 获取所有玩家
     */
    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }
}

