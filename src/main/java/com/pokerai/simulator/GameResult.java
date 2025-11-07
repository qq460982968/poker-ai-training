package com.pokerai.simulator;

import com.pokerai.model.Card;
import com.pokerai.model.Player;
import com.pokerai.rules.GameRules;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 单局游戏结果，包括每道对比和总分汇总
 */
public class GameResult {

    public static class PlayerDaoScore {
        private final Player player;
        private final List<Card> originalCards;
        private final GameRules.HandResult handResult;
        private int points;

        PlayerDaoScore(Player player, List<Card> originalCards, GameRules.HandResult handResult) {
            this.player = player;
            this.originalCards = Collections.unmodifiableList(new ArrayList<>(originalCards));
            this.handResult = handResult;
        }

        void setPoints(int points) {
            this.points = points;
        }

        public Player getPlayer() {
            return player;
        }

        public List<Card> getOriginalCards() {
            return originalCards;
        }

        public GameRules.HandResult getHandResult() {
            return handResult;
        }

        public int getPoints() {
            return points;
        }
    }

    public static class DaoResult {
        private final String name;
        private final List<PlayerDaoScore> rankings;

        DaoResult(String name, List<PlayerDaoScore> rankings) {
            this.name = name;
            this.rankings = Collections.unmodifiableList(new ArrayList<>(rankings));
        }

        public String getName() {
            return name;
        }

        public List<PlayerDaoScore> getRankings() {
            return rankings;
        }
    }

    private final List<Player> playerOrder;
    private final List<DaoResult> daoResults;
    private final Map<Player, Integer> totalPoints;
    private final Map<Player, List<Integer>> daoPoints;
    private final List<Player> winners;
    private final Map<Player, List<Card>> initialHands;

    GameResult(List<Player> playerOrder,
               List<DaoResult> daoResults,
               Map<Player, Integer> totalPoints,
               Map<Player, List<Integer>> daoPoints,
               List<Player> winners,
               Map<Player, List<Card>> initialHands) {
        this.playerOrder = Collections.unmodifiableList(new ArrayList<>(playerOrder));
        this.daoResults = Collections.unmodifiableList(new ArrayList<>(daoResults));
        this.totalPoints = Collections.unmodifiableMap(new LinkedHashMap<>(totalPoints));

        Map<Player, List<Integer>> breakdownCopy = new LinkedHashMap<>();
        daoPoints.forEach((player, values) ->
            breakdownCopy.put(player, Collections.unmodifiableList(new ArrayList<>(values)))
        );
        this.daoPoints = Collections.unmodifiableMap(breakdownCopy);
        this.winners = Collections.unmodifiableList(new ArrayList<>(winners));
        Map<Player, List<Card>> handsCopy = new LinkedHashMap<>();
        initialHands.forEach((p, hand) -> handsCopy.put(p, Collections.unmodifiableList(new ArrayList<>(hand))));
        this.initialHands = Collections.unmodifiableMap(handsCopy);
    }

    public List<Player> getPlayerOrder() {
        return playerOrder;
    }

    public List<DaoResult> getDaoResults() {
        return daoResults;
    }

    public Map<Player, Integer> getTotalPoints() {
        return totalPoints;
    }

    public Map<Player, List<Integer>> getDaoPoints() {
        return daoPoints;
    }

    public List<Player> getWinners() {
        return winners;
    }

    public Map<Player, List<Card>> getInitialHands() {
        return initialHands;
    }
}


