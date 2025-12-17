package com.pokerai.rules;

import com.pokerai.model.Card;
import java.util.*;

/**
 * 游戏规则引擎
 * 定义扑克牌游戏规则和牌型判断
 */
public class GameRules {
    
    /**
     * 牌型枚举
     */
    public enum HandType {
        HIGH_CARD(1, "散牌"),
        PAIR(2, "一对"),
        TWO_PAIR(3, "两对"),
        THREE_OF_A_KIND(4, "三条"),
        STRAIGHT(5, "顺子"),
        FLUSH(6, "同花"),
        FULL_HOUSE(7, "葫芦"),
        FOUR_OF_A_KIND(8, "四条"),
        STRAIGHT_FLUSH(9, "同花顺"),
        ROYAL_FLUSH(10, "皇家同花顺");
        
        private final int rank;
        private final String name;
        
        HandType(int rank, String name) {
            this.rank = rank;
            this.name = name;
        }
        
        public int getRank() {
            return rank;
        }
        
        public String getName() {
            return name;
        }
    }
    
    /**
     * 牌型结果
     */
    public static class HandResult {
        private final HandType handType;
        private final int score;
        private final List<Card> cards;
        
        public HandResult(HandType handType, int score, List<Card> cards) {
            this.handType = handType;
            this.score = score;
            this.cards = new ArrayList<>(cards);
        }
        
        public HandType getHandType() {
            return handType;
        }
        
        public int getScore() {
            return score;
        }
        
        public List<Card> getCards() {
            return new ArrayList<>(cards);
        }
    }
    
    /**
     * 评估一组牌（3张）的牌型
     * 这是核心规则：玩家需要将9张牌分成3组，每组3张
     */
    public static HandResult evaluateHand(List<Card> cards) {
        if (cards.size() != 3) {
            throw new IllegalArgumentException("必须正好3张牌");
        }

        long jokerCount = cards.stream().filter(Card::isJoker).count();
        if (jokerCount == 0) {
            return evaluateConcreteHand(cards);
        }
        return evaluateWithJokers(cards);
    }

    private static HandResult evaluateConcreteHand(List<Card> cards) {
        List<Card> sorted = new ArrayList<>(cards);
        sorted.sort(Comparator.comparingInt(Card::getValue));

        boolean isFlush = isFlush(sorted);
        boolean isStraight = isStraight(sorted);

        if (isFlush && isStraight) {
            if (isRoyalStraight(sorted)) {
                return new HandResult(HandType.ROYAL_FLUSH, 900 + encodeValues(sorted), sorted);
            }
            return new HandResult(HandType.STRAIGHT_FLUSH, 800 + encodeValues(sorted), sorted);
        }

        if (isThreeOfAKind(sorted)) {
            return new HandResult(HandType.THREE_OF_A_KIND, 1000 + sorted.get(0).getValue(), sorted);
        }

        if (isFlush) {
            return new HandResult(HandType.FLUSH, 600 + encodeValues(sorted), sorted);
        }

        if (isStraight) {
            return new HandResult(HandType.STRAIGHT, 500 + sorted.get(2).getValue(), sorted);
        }

        if (isPair(sorted)) {
            int pairValue = sorted.get(1).getValue();
            int kicker = sorted.get(0).getValue() == pairValue ? sorted.get(2).getValue() : sorted.get(0).getValue();
            return new HandResult(HandType.PAIR, 200 + pairValue * 20 + kicker, sorted);
        }

        return new HandResult(HandType.HIGH_CARD, encodeValues(sorted), sorted);
    }

    private static HandResult evaluateWithJokers(List<Card> cards) {
        BestHandHolder holder = new BestHandHolder();
        enumerateAssignments(cards, 0, new ArrayList<>(), holder);
        if (holder.bestResult == null) {
            throw new IllegalStateException("无法评估包含万能牌的组合");
        }
        return holder.bestResult;
    }

    private static void enumerateAssignments(List<Card> original, int index, List<Card> current, BestHandHolder holder) {
        if (index == original.size()) {
            HandResult result = evaluateConcreteHand(current);
            if (holder.bestResult == null || compareHands(result, holder.bestResult) > 0) {
                holder.bestResult = result;
            }
            return;
        }

        Card card = original.get(index);
        if (!card.isJoker()) {
            current.add(card);
            enumerateAssignments(original, index + 1, current, holder);
            current.remove(current.size() - 1);
        } else {
            for (Card.Rank rank : Card.Rank.values()) {
                for (Card.Suit suit : Card.Suit.values()) {
                    current.add(new Card(suit, rank));
                    enumerateAssignments(original, index + 1, current, holder);
                    current.remove(current.size() - 1);
                }
            }
        }
    }

    private static class BestHandHolder {
        private HandResult bestResult;
    }
    
    /**
     * 判断是否为同花
     */
    private static boolean isFlush(List<Card> cards) {
        Card.Suit suit = cards.get(0).getSuit();
        return cards.stream().allMatch(c -> c.getSuit() == suit);
    }
    
    /**
     * 判断是否为顺子
     */
    private static boolean isStraight(List<Card> cards) {
        int v0 = cards.get(0).getValue();
        int v1 = cards.get(1).getValue();
        int v2 = cards.get(2).getValue();
        return v1 == v0 + 1 && v2 == v1 + 1;
    }

    private static boolean isRoyalStraight(List<Card> cards) {
        return cards.get(0).getValue() == 12 && isStraight(cards);
    }

    private static boolean isThreeOfAKind(List<Card> cards) {
        return cards.get(0).getValue() == cards.get(1).getValue()
            && cards.get(1).getValue() == cards.get(2).getValue();
    }

    private static boolean isPair(List<Card> cards) {
        return cards.get(0).getValue() == cards.get(1).getValue()
            || cards.get(1).getValue() == cards.get(2).getValue();
    }

    private static int encodeValues(List<Card> cards) {
        return cards.get(2).getValue() * 400 + cards.get(1).getValue() * 20 + cards.get(0).getValue();
    }
    
    /**
     * 获取高牌值
     */
    /**
     * 获取花色的优先级（用于比较）
     * 黑桃(SPADES) > 红桃(HEARTS) > 梅花(CLUBS) > 方块(DIAMONDS)
     */
    private static int getSuitPriority(Card.Suit suit) {
        if (suit == null) {
            return -1; // 万能牌优先级最低
        }
        switch (suit) {
            case SPADES:   return 4;  // 黑桃最大
            case HEARTS:   return 3;  // 红桃
            case CLUBS:    return 2;  // 梅花
            case DIAMONDS: return 1;  // 方块最小
            default:       return 0;
        }
    }
    
    /**
     * 比较两个牌组的结果
     * @return 正数表示result1更强，负数表示result2更强，0表示平局
     */
    public static int compareHands(HandResult result1, HandResult result2) {
        int rankCompare = Integer.compare(result1.getHandType().getRank(), result2.getHandType().getRank());
        if (rankCompare != 0) {
            return rankCompare;
        }
        int scoreCompare = Integer.compare(result1.getScore(), result2.getScore());
        if (scoreCompare != 0) {
            return scoreCompare;
        }
        return compareByCards(result1.getCards(), result2.getCards(), result1.getHandType());
    }

    /**
     * 比较两组牌（考虑花色优先级）
     * @param cards1 第一组牌
     * @param cards2 第二组牌
     * @param handType 牌型（用于特殊处理同花）
     */
    private static int compareByCards(List<Card> cards1, List<Card> cards2, HandType handType) {
        // 如果是同花，优先比较花色（黑桃 > 红桃 > 梅花 > 方块）
        if (handType == HandType.FLUSH || handType == HandType.STRAIGHT_FLUSH || handType == HandType.ROYAL_FLUSH) {
            Card.Suit suit1 = cards1.get(0).getSuit();
            Card.Suit suit2 = cards2.get(0).getSuit();
            int suitCompare = Integer.compare(getSuitPriority(suit1), getSuitPriority(suit2));
            if (suitCompare != 0) {
                return suitCompare;
            }
            // 如果花色相同，继续比较点数
        }
        
        List<Card> sorted1 = new ArrayList<>(cards1);
        List<Card> sorted2 = new ArrayList<>(cards2);

        // 先按点数排序，再按花色优先级排序
        Comparator<Card> comparator = Comparator
            .comparingInt(Card::getValue)
            .thenComparing(card -> getSuitPriority(card.getSuit()));

        sorted1.sort(comparator);
        sorted2.sort(comparator);

        // 从大到小比较（先比较最大的牌）
        for (int i = sorted1.size() - 1; i >= 0; i--) {
            int valueCompare = Integer.compare(sorted1.get(i).getValue(), sorted2.get(i).getValue());
            if (valueCompare != 0) {
                return valueCompare;
            }
            // 如果点数相同，比较花色优先级
            int suitCompare = Integer.compare(
                getSuitPriority(sorted1.get(i).getSuit()),
                getSuitPriority(sorted2.get(i).getSuit())
            );
            if (suitCompare != 0) {
                return suitCompare;
            }
        }
        return 0;
    }
    
    /**
     * 评估玩家的完整组合（3组牌）
     * 返回总得分
     */
    public static List<List<Card>> sortGroupsDescending(List<List<Card>> groups) {
        if (groups.size() != 3) {
            throw new IllegalArgumentException("必须正好3组牌");
        }

        List<GroupEvaluation> evaluations = new ArrayList<>();
        for (List<Card> group : groups) {
            HandResult result = evaluateHand(group);
            evaluations.add(new GroupEvaluation(group, result));
        }

        evaluations.sort((a, b) -> compareHands(b.result, a.result));

        List<List<Card>> sorted = new ArrayList<>();
        for (GroupEvaluation evaluation : evaluations) {
            sorted.add(new ArrayList<>(evaluation.group));
        }
        return sorted;
    }

    public static List<List<Card>> sortGroupsAscending(List<List<Card>> groups) {
        if (groups.size() != 3) {
            throw new IllegalArgumentException("必须正好3组牌");
        }

        List<GroupEvaluation> evaluations = new ArrayList<>();
        for (List<Card> group : groups) {
            HandResult result = evaluateHand(group);
            evaluations.add(new GroupEvaluation(group, result));
        }

        evaluations.sort((a, b) -> compareHands(a.result, b.result));

        List<List<Card>> sorted = new ArrayList<>();
        for (GroupEvaluation evaluation : evaluations) {
            sorted.add(new ArrayList<>(evaluation.group));
        }
        return sorted;
    }

    private static class GroupEvaluation {
        private final List<Card> group;
        private final HandResult result;

        private GroupEvaluation(List<Card> group, HandResult result) {
            this.group = new ArrayList<>(group);
            this.result = result;
        }
    }

    public static int evaluatePlayerGroups(List<List<Card>> groups) {
        if (groups.size() != 3) {
            throw new IllegalArgumentException("必须正好3组牌");
        }

        int totalScore = 0;
        for (List<Card> group : groups) {
            HandResult result = evaluateHand(group);
            totalScore += result.getScore();
        }

        return totalScore;
    }
    
    /**
     * 判断玩家是否获胜（比较3组牌的总得分）
     */
    public static boolean isWinner(List<List<Card>> playerGroups, List<List<Card>> opponentGroups) {
        int playerScore = evaluatePlayerGroups(playerGroups);
        int opponentScore = evaluatePlayerGroups(opponentGroups);
        return playerScore > opponentScore;
    }
}


