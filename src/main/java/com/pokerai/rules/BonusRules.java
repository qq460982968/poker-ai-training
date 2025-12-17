package com.pokerai.rules;

import com.pokerai.model.Card;
import com.pokerai.rules.GameRules.HandResult;
import com.pokerai.rules.GameRules.HandType;
import java.util.*;

/**
 * 喜钱规则 - 定义特定牌型的额外得分奖励
 */
public class BonusRules {
    
    /**
     * 喜牌类型
     */
    public enum BonusType {
        SAN_QING("三清", 6),                    // 头中尾道全是同花或三条或同花顺
        SAN_SHUN("三顺", 6),                   // 头中尾道全是顺子或同花顺
        SHUANG_TONG_HUA_SHUN("双同花顺", 6),    // 中尾道全是同花顺
        SHUANG_SAN_TIAO("双三条", 6),          // 中尾道全是三条
        QUAN_HONG("全红", 6),                  // 所有牌全是红桃或方片或大小王
        QUAN_HEI("全黑", 6),                  // 所有牌全是黑桃或梅花或大小王
        SAN_TONG_HUA_SHUN("三同花顺", 12),      // 头中尾道全是同花顺
        SAN_DUN_SAN_TIAO("三敦三条", 20),      // 头中尾道全是三条
        YI_FU_ZHA_DAN("一副炸弹", 20),          // 有4张相同大小的任意牌且其中三张组成一道牌
        JIU_SHUN_ZI("九顺子", 30),             // 9张相连的牌
        SHUANG_ZHA_DAN("双炸弹", 40),          // 有2组4张相同大小的牌且分别组成一个三条
        JIU_LIAN_TONG_HUA_SHUN("九连同花顺", 40); // 9张相连的相同花色的牌
        
        private final String name;
        private final int bonus;
        
        BonusType(String name, int bonus) {
            this.name = name;
            this.bonus = bonus;
        }
        
        public String getName() {
            return name;
        }
        
        public int getBonus() {
            return bonus;
        }
    }
    
    /**
     * 喜牌结果
     */
    public static class BonusResult {
        private final BonusType bonusType;
        private final List<Card> allCards; // 所有9张牌
        private final int bonus;
        
        public BonusResult(BonusType bonusType, List<Card> allCards, int bonus) {
            this.bonusType = bonusType;
            this.allCards = new ArrayList<>(allCards);
            this.bonus = bonus;
        }
        
        public BonusType getBonusType() {
            return bonusType;
        }
        
        public List<Card> getAllCards() {
            return new ArrayList<>(allCards);
        }
        
        public int getBonus() {
            return bonus;
        }
    }
    
    /**
     * 检查玩家的所有牌组，返回最大得分的喜牌结果（如果同时符合多个规则，只取最大得分的一个）
     * @param groups 玩家的3组牌（已排序：头道、中道、尾道）
     * @param allCards 玩家的所有9张牌
     * @return 喜牌结果，如果不符合则返回null
     */
    public static BonusResult checkPlayerBonus(List<List<Card>> groups, List<Card> allCards) {
        if (groups.size() != 3 || allCards.size() != 9) {
            return null;
        }
        
        List<Card> head = groups.get(0);  // 头道
        List<Card> middle = groups.get(1); // 中道
        List<Card> tail = groups.get(2);   // 尾道
        
        // 评估每道牌的牌型
        HandResult headResult = GameRules.evaluateHand(head);
        HandResult middleResult = GameRules.evaluateHand(middle);
        HandResult tailResult = GameRules.evaluateHand(tail);
        
        HandType headType = headResult.getHandType();
        HandType middleType = middleResult.getHandType();
        HandType tailType = tailResult.getHandType();
        
        List<BonusResult> allBonuses = new ArrayList<>();
        
        // 1. 【九连同花顺】9张相连的相同花色的牌 - 40分
        BonusResult bonus = checkJiuLianTongHuaShun(allCards);
        if (bonus != null) allBonuses.add(bonus);
        
        // 2. 【双炸弹】有2组4张相同大小的牌且分别组成一个三条 - 40分
        bonus = checkShuangZhaDan(groups, allCards);
        if (bonus != null) allBonuses.add(bonus);
        
        // 3. 【九顺子】9张相连的牌 - 30分
        bonus = checkJiuShunZi(allCards);
        if (bonus != null) allBonuses.add(bonus);
        
        // 4. 【三敦三条】头中尾道全是三条 - 20分
        if (headType == HandType.THREE_OF_A_KIND && 
            middleType == HandType.THREE_OF_A_KIND && 
            tailType == HandType.THREE_OF_A_KIND) {
            allBonuses.add(new BonusResult(BonusType.SAN_DUN_SAN_TIAO, allCards, 20));
        }
        
        // 5. 【一副炸弹】有4张相同大小的任意牌且其中三张组成一道牌 - 20分
        bonus = checkYiFuZhaDan(groups, allCards);
        if (bonus != null) allBonuses.add(bonus);
        
        // 6. 【三同花顺】头中尾道全是同花顺 - 12分
        if (headType == HandType.STRAIGHT_FLUSH || headType == HandType.ROYAL_FLUSH) {
            if ((middleType == HandType.STRAIGHT_FLUSH || middleType == HandType.ROYAL_FLUSH) &&
                (tailType == HandType.STRAIGHT_FLUSH || tailType == HandType.ROYAL_FLUSH)) {
                allBonuses.add(new BonusResult(BonusType.SAN_TONG_HUA_SHUN, allCards, 12));
            }
        }
        
        // 7. 【三清】头中尾道全是同花或三条或同花顺 - 6分
        boolean headQing = headType == HandType.FLUSH || headType == HandType.THREE_OF_A_KIND || 
                          headType == HandType.STRAIGHT_FLUSH || headType == HandType.ROYAL_FLUSH;
        boolean middleQing = middleType == HandType.FLUSH || middleType == HandType.THREE_OF_A_KIND || 
                            middleType == HandType.STRAIGHT_FLUSH || middleType == HandType.ROYAL_FLUSH;
        boolean tailQing = tailType == HandType.FLUSH || tailType == HandType.THREE_OF_A_KIND || 
                          tailType == HandType.STRAIGHT_FLUSH || tailType == HandType.ROYAL_FLUSH;
        if (headQing && middleQing && tailQing) {
            allBonuses.add(new BonusResult(BonusType.SAN_QING, allCards, 6));
        }
        
        // 8. 【三顺】头中尾道全是顺子或同花顺 - 6分
        boolean headShun = headType == HandType.STRAIGHT || headType == HandType.STRAIGHT_FLUSH || 
                          headType == HandType.ROYAL_FLUSH;
        boolean middleShun = middleType == HandType.STRAIGHT || middleType == HandType.STRAIGHT_FLUSH || 
                            middleType == HandType.ROYAL_FLUSH;
        boolean tailShun = tailType == HandType.STRAIGHT || tailType == HandType.STRAIGHT_FLUSH || 
                          tailType == HandType.ROYAL_FLUSH;
        if (headShun && middleShun && tailShun) {
            allBonuses.add(new BonusResult(BonusType.SAN_SHUN, allCards, 6));
        }
        
        // 9. 【双同花顺】中尾道全是同花顺 - 6分
        if ((middleType == HandType.STRAIGHT_FLUSH || middleType == HandType.ROYAL_FLUSH) &&
            (tailType == HandType.STRAIGHT_FLUSH || tailType == HandType.ROYAL_FLUSH)) {
            allBonuses.add(new BonusResult(BonusType.SHUANG_TONG_HUA_SHUN, allCards, 6));
        }
        
        // 10. 【双三条】中尾道全是三条 - 6分
        if (middleType == HandType.THREE_OF_A_KIND && tailType == HandType.THREE_OF_A_KIND) {
            allBonuses.add(new BonusResult(BonusType.SHUANG_SAN_TIAO, allCards, 6));
        }
        
        // 11. 【全红】所有牌全是红桃或方片或大小王 - 6分
        if (checkAllRed(allCards)) {
            allBonuses.add(new BonusResult(BonusType.QUAN_HONG, allCards, 6));
        }
        
        // 12. 【全黑】所有牌全是黑桃或梅花或大小王 - 6分
        if (checkAllBlack(allCards)) {
            allBonuses.add(new BonusResult(BonusType.QUAN_HEI, allCards, 6));
        }
        
        // 如果同时符合多个规则，只取最大得分的一个
        if (allBonuses.isEmpty()) {
            return null;
        }
        
        // 按得分降序排序，返回得分最高的
        allBonuses.sort((a, b) -> Integer.compare(b.getBonus(), a.getBonus()));
        return allBonuses.get(0);
    }
    
    /**
     * 检查是否为九连同花顺（9张相连的相同花色的牌）
     */
    private static BonusResult checkJiuLianTongHuaShun(List<Card> allCards) {
        // 检查是否所有牌都是同一花色（大小王可以当作任意花色）
        Card.Suit targetSuit = null;
        List<Integer> values = new ArrayList<>();
        
        for (Card card : allCards) {
            if (card.isJoker()) {
                continue; // 大小王可以当作任意花色和点数
            }
            if (targetSuit == null) {
                targetSuit = card.getSuit();
            } else if (card.getSuit() != targetSuit) {
                return null; // 花色不一致
            }
            values.add(card.getValue());
        }
        
        if (values.size() < 9) {
            return null; // 必须有足够的非万能牌
        }
        
        // 检查是否9张相连
        Collections.sort(values);
        if (isConsecutive(values, 9)) {
            return new BonusResult(BonusType.JIU_LIAN_TONG_HUA_SHUN, allCards, 40);
        }
        
        return null;
    }
    
    /**
     * 检查是否为双炸弹（有2组4张相同大小的牌且分别组成一个三条）
     */
    private static BonusResult checkShuangZhaDan(List<List<Card>> groups, List<Card> allCards) {
        // 统计每种点数的牌数
        Map<Integer, Integer> rankCount = new HashMap<>();
        for (Card card : allCards) {
            if (!card.isJoker()) {
                rankCount.put(card.getValue(), rankCount.getOrDefault(card.getValue(), 0) + 1);
            }
        }
        
        // 找出有4张相同点数的牌
        List<Integer> fourOfAKindRanks = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : rankCount.entrySet()) {
            if (entry.getValue() >= 4) {
                fourOfAKindRanks.add(entry.getKey());
            }
        }
        
        if (fourOfAKindRanks.size() < 2) {
            return null; // 至少需要2组4张相同点数的牌
        }
        
        // 检查这2组4张相同点数的牌是否分别组成了三条
        int threeOfAKindCount = 0;
        for (List<Card> group : groups) {
            HandResult result = GameRules.evaluateHand(group);
            if (result.getHandType() == HandType.THREE_OF_A_KIND) {
                // 检查这个三条是否使用了4张相同点数中的3张
                int groupRank = -1;
                for (Card card : group) {
                    if (!card.isJoker()) {
                        if (groupRank == -1) {
                            groupRank = card.getValue();
                        } else if (card.getValue() != groupRank) {
                            groupRank = -1;
                            break;
                        }
                    }
                }
                if (groupRank != -1 && fourOfAKindRanks.contains(groupRank)) {
                    threeOfAKindCount++;
                }
            }
        }
        
        if (threeOfAKindCount >= 2) {
            return new BonusResult(BonusType.SHUANG_ZHA_DAN, allCards, 40);
        }
        
        return null;
    }
    
    /**
     * 检查是否为九顺子（9张相连的牌）
     */
    private static BonusResult checkJiuShunZi(List<Card> allCards) {
        List<Integer> values = new ArrayList<>();
        int jokerCount = 0;
        
        for (Card card : allCards) {
            if (card.isJoker()) {
                jokerCount++;
            } else {
                values.add(card.getValue());
            }
        }
        
        if (values.size() + jokerCount < 9) {
            return null;
        }
        
        Collections.sort(values);
        
        // 检查是否9张相连（考虑万能牌）
        if (isConsecutiveWithJokers(values, jokerCount, 9)) {
            return new BonusResult(BonusType.JIU_SHUN_ZI, allCards, 30);
        }
        
        return null;
    }
    
    /**
     * 检查是否为一副炸弹（有4张相同大小的任意牌且其中三张组成一道牌）
     */
    private static BonusResult checkYiFuZhaDan(List<List<Card>> groups, List<Card> allCards) {
        // 统计每种点数的牌数
        Map<Integer, Integer> rankCount = new HashMap<>();
        for (Card card : allCards) {
            if (!card.isJoker()) {
                rankCount.put(card.getValue(), rankCount.getOrDefault(card.getValue(), 0) + 1);
            }
        }
        
        // 找出有4张相同点数的牌
        for (Map.Entry<Integer, Integer> entry : rankCount.entrySet()) {
            if (entry.getValue() >= 4) {
                int rank = entry.getKey();
                // 检查这4张牌中的3张是否组成了三条
                for (List<Card> group : groups) {
                    HandResult result = GameRules.evaluateHand(group);
                    if (result.getHandType() == HandType.THREE_OF_A_KIND) {
                        // 检查这个三条是否使用了这4张相同点数中的3张
                        int countInGroup = 0;
                        for (Card card : group) {
                            if (!card.isJoker() && card.getValue() == rank) {
                                countInGroup++;
                            }
                        }
                        if (countInGroup == 3) {
                            return new BonusResult(BonusType.YI_FU_ZHA_DAN, allCards, 20);
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * 检查是否全红（所有牌全是红桃或方片或大小王）
     */
    private static boolean checkAllRed(List<Card> allCards) {
        for (Card card : allCards) {
            if (card.isJoker()) {
                continue; // 大小王算作红色
            }
            if (card.getSuit() != Card.Suit.HEARTS && card.getSuit() != Card.Suit.DIAMONDS) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 检查是否全黑（所有牌全是黑桃或梅花或大小王）
     */
    private static boolean checkAllBlack(List<Card> allCards) {
        for (Card card : allCards) {
            if (card.isJoker()) {
                continue; // 大小王算作黑色
            }
            if (card.getSuit() != Card.Suit.SPADES && card.getSuit() != Card.Suit.CLUBS) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 检查数字列表是否连续
     */
    private static boolean isConsecutive(List<Integer> values, int expectedLength) {
        if (values.size() != expectedLength) {
            return false;
        }
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) != values.get(i - 1) + 1) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 检查数字列表是否连续（考虑万能牌）
     */
    private static boolean isConsecutiveWithJokers(List<Integer> values, int jokerCount, int expectedLength) {
        if (values.size() + jokerCount < expectedLength) {
            return false;
        }
        
        // 尝试用万能牌填充空缺
        int gaps = 0;
        for (int i = 1; i < values.size(); i++) {
            int diff = values.get(i) - values.get(i - 1);
            if (diff > 1) {
                gaps += diff - 1;
            }
        }
        
        // 还需要填充前后
        int minValue = values.isEmpty() ? 0 : values.get(0);
        int maxValue = values.isEmpty() ? 0 : values.get(values.size() - 1);
        int neededBefore = Math.max(0, 6 - minValue); // 最小是6
        int neededAfter = Math.max(0, maxValue - 14); // 最大是A(14)
        
        return gaps + neededBefore + neededAfter <= jokerCount && 
               (maxValue - minValue + 1 + gaps + neededBefore + neededAfter) == expectedLength;
    }
}
