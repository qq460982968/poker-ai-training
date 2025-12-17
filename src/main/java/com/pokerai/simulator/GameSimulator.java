package com.pokerai.simulator;

import com.pokerai.ai.AIPlayer;
import com.pokerai.model.Card;
import com.pokerai.model.Deck;
import com.pokerai.model.Player;
import com.pokerai.rules.BonusRules;
import com.pokerai.rules.GameRules;

import java.util.*;

/**
 * 游戏模拟器 - 模拟4人对战
 */
public class GameSimulator {

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
     *
     * @return AI玩家是否获胜
     */
    public boolean playOneGame() {
        // 重置牌堆并洗牌
        deck.reset();

        // 给每个玩家发9张牌
        for (Player player : players) {
            List<Card> cards = deck.deal(9);
            player.receiveCards(cards);
        }

        // AI玩家决策
        List<List<Card>> aiGroups = aiPlayer.decideCardGroups();
        aiPlayer.setCardGroups(aiGroups);

        // 其他玩家使用简单策略（随机分组或贪心策略）
        for (int i = 1; i < players.size(); i++) {
            Player player = players.get(i);
            List<List<Card>> groups = generateSimpleStrategy(player.getHand());
            player.setCardGroups(groups);
        }

        // 比较所有玩家的得分（包含喜钱）
        Map<Player, Integer> scores = new HashMap<>();
        Map<Player, Integer> bonusScores = new HashMap<>(); // 喜钱得分
        Map<Player, BonusRules.BonusResult> playerBonuses = new HashMap<>(); // 每个玩家的喜牌（只取最大得分的）

        for (Player player : players) {
            // 基础得分
            int baseScore = GameRules.evaluatePlayerGroups(player.getCardGroups());

            // 喜钱得分（检查所有喜牌规则，只取最大得分的一个）
            BonusRules.BonusResult bonus = BonusRules.checkPlayerBonus(
                    player.getCardGroups(),
                    player.getHand()
            );
            int totalBonus = (bonus != null) ? bonus.getBonus() : 0;

            // 总得分 = 基础得分 + 喜钱得分
            // 注意：喜钱需要从其他玩家扣除，这里先计算每个玩家的总得分
            scores.put(player, baseScore + totalBonus);
            bonusScores.put(player, totalBonus);
            playerBonuses.put(player, bonus);
        }

        // 计算喜钱：每个玩家的喜钱从其他3个玩家扣除
        // 例如：玩家A获得10分喜钱，则玩家B、C、D各扣10/3分（或按规则分配）
        Map<Player, Integer> netBonusScores = new HashMap<>();
        for (Player player : players) {
            int myBonus = bonusScores.get(player);
            int deductedFromOthers = 0;

            // 从其他玩家的喜钱中扣除
            for (Player other : players) {
                if (other != player) {
                    deductedFromOthers += bonusScores.get(other);
                }
            }

            // 净喜钱得分 = 自己的喜钱 - 被扣除的喜钱
            netBonusScores.put(player, myBonus - deductedFromOthers);
        }

        // 重新计算总得分（包含净喜钱）
        for (Player player : players) {
            int baseScore = GameRules.evaluatePlayerGroups(player.getCardGroups());
            scores.put(player, baseScore + netBonusScores.get(player));
        }

        // 找出得分最高的玩家
        Player winner = players.get(0);
        int maxScore = scores.get(winner);
        for (Player player : players) {
            int score = scores.get(player);
            if (score > maxScore) {
                maxScore = score;
                winner = player;
            }
        }

        // 检查是否有平局（多个玩家得分相同）
        List<Player> winners = new ArrayList<>();
        for (Player player : players) {
            if (scores.get(player) == maxScore) {
                winners.add(player);
            }
        }

        // 如果AI玩家是获胜者之一，算作获胜
        boolean aiWon = winners.contains(aiPlayer);

        // AI学习
        if (aiWon) {
            aiPlayer.learn(aiGroups, true, maxScore);
            aiPlayer.addWin();
        } else {
            aiPlayer.learn(aiGroups, false, scores.get(aiPlayer));
            aiPlayer.addGame();
        }

        // 更新其他玩家的统计
        for (Player player : players) {
            if (player != aiPlayer) {
                if (winners.contains(player)) {
                    player.addWin();
                } else {
                    player.addGame();
                }

            }
        }

        return aiWon;
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

    /**
     * 执行一局游戏并输出详细记录（包含喜钱明细）
     */
    public void playOneGameWithDetails() {
        // 重置牌堆并洗牌
        deck.reset();

        // 给每个玩家发9张牌
        for (Player player : players) {
            List<Card> cards = deck.deal(9);
            player.receiveCards(cards);
        }

        // AI玩家决策
        List<List<Card>> aiGroups = GameRules.sortGroupsAscending(aiPlayer.decideCardGroups());
        aiPlayer.setCardGroups(aiGroups);

        // 其他玩家使用简单策略
        for (int i = 1; i < players.size(); i++) {
            Player player = players.get(i);
            List<List<Card>> groups = GameRules.sortGroupsAscending(generateSimpleStrategy(player.getHand()));
            player.setCardGroups(groups);
            // 基于牌型强度判断是否投降：三道全为散牌则投降
            GameRules.HandResult h0 = GameRules.evaluateHand(groups.get(0));
            GameRules.HandResult h1 = GameRules.evaluateHand(groups.get(1));
            GameRules.HandResult h2 = GameRules.evaluateHand(groups.get(2));
            boolean shouldSurrender =
                    h0.getHandType() == GameRules.HandType.HIGH_CARD &&
                            h1.getHandType() == GameRules.HandType.HIGH_CARD &&
                            h2.getHandType() == GameRules.HandType.HIGH_CARD;
            player.setSurrendered(shouldSurrender);
        }

        // 基于AI的牌型强度判断AI是否投降（一致规则）
        {
            List<List<Card>> g = aiPlayer.getCardGroups();
            GameRules.HandResult h0 = GameRules.evaluateHand(g.get(0));
            GameRules.HandResult h1 = GameRules.evaluateHand(g.get(1));
            GameRules.HandResult h2 = GameRules.evaluateHand(g.get(2));
            boolean shouldSurrender =
                    h0.getHandType() == GameRules.HandType.HIGH_CARD &&
                            h1.getHandType() == GameRules.HandType.HIGH_CARD &&
                            h2.getHandType() == GameRules.HandType.HIGH_CARD;
            aiPlayer.setSurrendered(shouldSurrender);
        }

        // 计算每道的排名得分（处理投降情况）
        Map<Player, List<Integer>> daoPoints = new HashMap<>(); // 每个玩家每道的得分

        // 初始化每道得分
        for (Player player : players) {
            daoPoints.put(player, new ArrayList<>(Arrays.asList(0, 0, 0)));
        }

        // 计算每道的排名
        for (int daoIndex = 0; daoIndex < 3; daoIndex++) {
            // 收集未投降玩家在这一道的牌
            List<PlayerDaoScore> rankings = new ArrayList<>();
            List<Player> surrenderedPlayers = new ArrayList<>();

            for (Player player : players) {
                if (player.isSurrendered()) {
                    surrenderedPlayers.add(player);
                    // 投降玩家每道固定输6分
                    daoPoints.get(player).set(daoIndex, -6);
                } else {
                    List<Card> group = player.getCardGroups().get(daoIndex);
                    GameRules.HandResult handResult = GameRules.evaluateHand(group);
                    rankings.add(new PlayerDaoScore(player, group, handResult));
                }
            }

            // 如果有未投降的玩家，进行排名
            if (!rankings.isEmpty()) {
                // 按牌型从大到小排序
                rankings.sort((a, b) -> {
                    int compare = GameRules.compareHands(b.getHandResult(), a.getHandResult());
                    if (compare != 0) {
                        return compare;
                    }
                    // 如果牌型相同，按玩家ID排序
                    return Integer.compare(a.getPlayer().getId(), b.getPlayer().getId());
                });

                // 计算得分：遵循用户给定的规则
                // 规则：
                // - 每个投降玩家固定 -6
                // - 其余未投降玩家（除赢家外）的罚分依次为：最差 -6，其次 -4，再次为-2（若仅一名未投降对手，则其为 -2）
                // - 赢家得分为其他所有人的负分之和（确保零和）
                // 为此，先为未投降玩家的非赢家分配罚分，再给赢家分配其余的正分
                // 排名列表rankings按强到弱排列（index 0 为赢家）
                // 给非赢家分配罚分
                int losersCount = Math.max(0, rankings.size() - 1);
                // 非赢家从最弱开始分配：-6，然后-2（如有）
                int[] remainingLoserPenalties = {-6,-4, -2};
                int penaltyIdx = 0;
                int totalNeg = surrenderedPlayers.size() * 6; // 投降者总负分
                // 分配未投降失败者的罚分（从最弱到较强）
                for (int i = rankings.size() - 1; i >= 1; i--) {
                    int penalty = 0;
                    if (penaltyIdx < remainingLoserPenalties.length) {
                        penalty = remainingLoserPenalties[penaltyIdx++];
                    } else {
                        // 若出现超过两名未投降失败者（理论上4人局不会），继续使用-2
                        penalty = -2;
                    }
                    PlayerDaoScore s = rankings.get(i);
                    s.setPoints(penalty);
                    daoPoints.get(s.getPlayer()).set(daoIndex, penalty);
                    totalNeg += -penalty; // penalty是负数，累加其绝对值
                }
                // 赢家得分 = 所有其他人的负分之和（包含所有投降者的-6）
                PlayerDaoScore winner = rankings.get(0);
                int winnerPoints = totalNeg;
                winner.setPoints(winnerPoints);
                daoPoints.get(winner.getPlayer()).set(daoIndex, winnerPoints);
            }
        }

        // 检查通关奖励（三道全胜）
        Map<Player, Integer> passRewards = new HashMap<>(); // 通关奖励
        for (Player player : players) {
            if (player.isSurrendered()) {
                passRewards.put(player, 0); // 投降玩家不获得通关奖励
                continue;
            }

            List<Integer> points = daoPoints.get(player);
            // 检查是否三道都排名第一
            boolean allWin = true;
            for (int i = 0; i < 3; i++) {
                int myPoint = points.get(i);
                // 检查这一道是否排名第一（得分最高）
                boolean isFirst = true;
                for (Player other : players) {
                    if (other != player && !other.isSurrendered()) {
                        int otherPoint = daoPoints.get(other).get(i);
                        if (otherPoint > myPoint) {
                            isFirst = false;
                            break;
                        }
                    }
                }
                if (!isFirst) {
                    allWin = false;
                    break;
                }
            }

            if (allWin) {
                // 三道全胜，获得通关奖励：每家6分（从其他未投降玩家扣除）
                int reward = 0;
                for (Player other : players) {
                    if (other != player && !other.isSurrendered()) {
                        reward += 6;
                    }
                }
                passRewards.put(player, reward);
            } else {
                passRewards.put(player, 0);
            }
        }

        // 计算基础得分（每道得分之和）
        Map<Player, Integer> baseScores = new HashMap<>();
        for (Player player : players) {
            List<Integer> points = daoPoints.get(player);
            int total = points.stream().mapToInt(Integer::intValue).sum();
            baseScores.put(player, total);
        }

        // 计算喜钱
        Map<Player, Integer> bonusScores = new HashMap<>();
        Map<Player, BonusRules.BonusResult> playerBonuses = new HashMap<>();

        for (Player player : players) {
            // 检查所有喜牌规则，只取最大得分的一个
            BonusRules.BonusResult bonus = BonusRules.checkPlayerBonus(
                    player.getCardGroups(),
                    player.getHand()
            );
            int totalBonus = (bonus != null) ? bonus.getBonus() : 0;

            bonusScores.put(player, totalBonus);
            playerBonuses.put(player, bonus);
        }

        // 计算净喜钱得分（每个玩家的喜钱从其他未投降玩家扣除）
        Map<Player, Integer> netBonusScores = new HashMap<>();
        int isSurrenderedCnt = 0;
        for (Player player : players) {
            if (player.isSurrendered()) {
                // 投降玩家不扣除喜钱
                netBonusScores.put(player, 0);
                isSurrenderedCnt++;
                continue;
            }

            int myBonus = bonusScores.get(player);
            int deductedFromOthers = 0;

            for (Player other : players) {
                if (other != player && !other.isSurrendered()) {
                    deductedFromOthers += bonusScores.get(other);
                }
            }

            netBonusScores.put(player, myBonus*(3-isSurrenderedCnt) - deductedFromOthers);
        }

        // 计算总得分 = 基础得分 + 净喜钱得分 + 通关奖励
        Map<Player, Integer> totalScores = new HashMap<>();
        for (Player player : players) {
            int total = baseScores.get(player) + netBonusScores.get(player) + passRewards.get(player);
            totalScores.put(player, total);
        }

        // 输出详细结果
        printDetailedResult(baseScores, bonusScores, netBonusScores, totalScores, playerBonuses, daoPoints, passRewards);

        // 找出获胜者
        int maxScore = totalScores.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<Player> winners = new ArrayList<>();
        for (Player player : players) {
            if (totalScores.get(player) == maxScore) {
                winners.add(player);
            }
        }

        // AI学习
        boolean aiWon = winners.contains(aiPlayer);
        if (aiWon) {
            aiPlayer.learn(aiGroups, true, maxScore);
            aiPlayer.addWin();
        } else {
            aiPlayer.learn(aiGroups, false, totalScores.get(aiPlayer));
            aiPlayer.addGame();
        }

        // 更新其他玩家的统计
        for (Player player : players) {
            if (player != aiPlayer) {
                if (winners.contains(player)) {
                    player.addWin();
                } else {
                    player.addGame();
                }
            }
        }
    }

    /**
     * 输出详细结果（包含喜钱明细和通关奖励）
     */
    private void printDetailedResult(Map<Player, Integer> baseScores,
                                     Map<Player, Integer> bonusScores,
                                     Map<Player, Integer> netBonusScores,
                                     Map<Player, Integer> totalScores,
                                     Map<Player, BonusRules.BonusResult> playerBonuses,
                                     Map<Player, List<Integer>> daoPoints,
                                     Map<Player, Integer> passRewards) {
        System.out.println("========================================");
        System.out.println("单局牌局结果");
        System.out.println("========================================");
        System.out.println();

        // 显示每个玩家的牌组和得分
        for (Player player : players) {
            String playerName = player.getName();
            if (player.isSurrendered()) {
                playerName += " [弃牌]";
            }
            System.out.println(playerName + "：");

            if (player.isSurrendered()) {
                // 投降玩家显示简化信息
                System.out.println("  原始牌: " + formatCards(player.getHand()));
                System.out.println("  配牌结果：");
                System.out.println("    头道: [弃牌] -6");
                System.out.println("    中道: [弃牌] -6");
                System.out.println("    尾道: [弃牌] -6");
                System.out.println("  喜钱 无（弃牌不扣除喜钱）");
                System.out.println("  通关奖励 无（弃牌不获得通关奖励）");
                int total = totalScores.get(player);
                System.out.printf("  得分 %+d%n", total);
            } else {
                System.out.println("  原始牌: " + formatCards(player.getHand()));
                System.out.println("  配牌结果：");

                List<List<Card>> groups = player.getCardGroups();
                List<Integer> points = daoPoints.get(player);

                for (int i = 0; i < groups.size(); i++) {
                    List<Card> group = groups.get(i);
                    GameRules.HandResult result = GameRules.evaluateHand(group);
                    int point = points.get(i);
                    System.out.printf("    %s: %s (%s) %+d%n",
                            getDaoName(i), formatCards(group), result.getHandType().getName(), point);
                }

                // 显示喜钱（显示原始喜钱得分，不是净喜钱）
                BonusRules.BonusResult bonusResult = playerBonuses.get(player);
                int bonus = bonusScores.get(player);
                if (bonusResult != null) {
                    System.out.printf("  喜钱类型 %s +%d   总喜钱  %d%n",
                            bonusResult.getBonusType().getName(),  bonus,netBonusScores.get(player));
                } else {
                    System.out.printf("  喜钱类型 无 总喜钱 %d%n", netBonusScores.get(player));
                }

                // 显示通关奖励
                int passReward = passRewards.get(player);
                if (passReward > 0) {
                    System.out.printf("  通关奖励 三道全胜 +%d%n", passReward);
                } else {
                    System.out.println("  通关奖励 无");
                }

                // 显示总得分
                int total = totalScores.get(player);
                System.out.printf("  得分 %+d%n", total);
            }
            System.out.println();
        }

        // 显示获胜者
        int maxScore = totalScores.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<String> winnerNames = new ArrayList<>();
        for (Player player : players) {
            if (totalScores.get(player) == maxScore) {
                winnerNames.add(player.getName());
            }
        }
        System.out.println("获胜玩家: " + (winnerNames.isEmpty() ? "无" : String.join("、", winnerNames)));
        System.out.println("========================================");
    }

    /**
     * 玩家单道得分（用于排序）
     */
    private static class PlayerDaoScore {
        private final Player player;
        private final List<Card> group;
        private final GameRules.HandResult handResult;
        private int points;

        public PlayerDaoScore(Player player, List<Card> group, GameRules.HandResult handResult) {
            this.player = player;
            this.group = new ArrayList<>(group);
            this.handResult = handResult;
        }

        public Player getPlayer() {
            return player;
        }

        public List<Card> getGroup() {
            return new ArrayList<>(group);
        }

        public GameRules.HandResult getHandResult() {
            return handResult;
        }

        public int getPoints() {
            return points;
        }

        public void setPoints(int points) {
            this.points = points;
        }
    }

    private String getDaoName(int index) {
        String[] names = {"头道", "中道", "尾道"};
        return index < names.length ? names[index] : "未知";
    }

    private String formatCards(List<Card> cards) {
        // ANSI颜色码
        final String RESET = "\u001B[0m";
        final String BLACK = "\u001B[35m";
        final String RED = "\u001B[31m";
        final String GREEN = "\u001B[32m";
        final String YELLOW = "\u001B[33m";
        final String MAGENTA = "\u001B[36m";

        List<String> labels = new ArrayList<>();
        for (Card card : cards) {
            String cardStr = card.toString();
            String color = RESET;

            if (card.isJoker()) {
                color = MAGENTA;
            } else {
                switch (card.getSuit()) {
                    case SPADES:
                        color = BLACK;
                        break;
                    case HEARTS:
                        color = RED;
                        break;
                    case CLUBS:
                        color = GREEN;
                        break;
                    case DIAMONDS:
                        color = YELLOW;
                        break;
                }
            }

            labels.add(color + "[" + cardStr + "]" + RESET);
        }
        return String.join(" ", labels);
    }
}



