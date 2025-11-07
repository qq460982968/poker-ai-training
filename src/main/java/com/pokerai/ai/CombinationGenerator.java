package com.pokerai.ai;

import com.pokerai.model.Card;
import java.util.*;

/**
 * 组合生成器 - 优化版本，使用更高效的算法生成所有可能的牌组合
 */
public class CombinationGenerator {
    
    /**
     * 生成所有可能的分组方式（9张牌分成3组，每组3张）
     * 使用组合数学优化
     */
    public static List<List<List<Card>>> generateAllCombinations(List<Card> cards) {
        if (cards.size() != 9) {
            throw new IllegalArgumentException("必须正好9张牌");
        }
        
        List<List<List<Card>>> result = new ArrayList<>();
        List<Card> remaining = new ArrayList<>(cards);
        
        // 使用递归生成所有组合
        generateGroups(remaining, new ArrayList<>(), result);
        
        return result;
    }
    
    /**
     * 递归生成分组
     */
    private static void generateGroups(List<Card> remaining, List<List<Card>> currentGroups,
                                      List<List<List<Card>>> result) {
        if (currentGroups.size() == 3) {
            // 已经生成3组，检查是否所有牌都已使用
            if (remaining.isEmpty()) {
                result.add(new ArrayList<>(currentGroups));
            }
            return;
        }
        
        // 如果剩余牌数不足以填满剩余组，提前返回
        int groupsNeeded = 3 - currentGroups.size();
        if (remaining.size() < groupsNeeded * 3) {
            return;
        }
        
        // 生成当前组的所有可能组合（从剩余牌中选择3张）
        List<List<Card>> combinations = generateCombinations(remaining, 3);
        
        for (List<Card> combination : combinations) {
            // 创建新的组
            List<Card> newGroup = new ArrayList<>(combination);
            currentGroups.add(newGroup);
            
            // 从剩余牌中移除已使用的牌
            List<Card> newRemaining = new ArrayList<>(remaining);
            for (Card card : combination) {
                newRemaining.remove(card);
            }
            
            // 递归生成下一组
            generateGroups(newRemaining, currentGroups, result);
            
            // 回溯
            currentGroups.remove(currentGroups.size() - 1);
        }
    }
    
    /**
     * 从列表中生成指定大小的所有组合
     */
    private static List<List<Card>> generateCombinations(List<Card> cards, int size) {
        List<List<Card>> result = new ArrayList<>();
        generateCombinationsRecursive(cards, size, 0, new ArrayList<>(), result);
        return result;
    }
    
    /**
     * 递归生成组合
     */
    private static void generateCombinationsRecursive(List<Card> cards, int size, int start,
                                                     List<Card> current, List<List<Card>> result) {
        if (current.size() == size) {
            result.add(new ArrayList<>(current));
            return;
        }
        
        for (int i = start; i < cards.size(); i++) {
            current.add(cards.get(i));
            generateCombinationsRecursive(cards, size, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
    
    /**
     * 快速生成方法：使用随机采样生成多种组合（用于性能优化）
     */
    public static List<List<List<Card>>> generateRandomCombinations(List<Card> cards, int count) {
        if (cards.size() != 9) {
            throw new IllegalArgumentException("必须正好9张牌");
        }
        
        List<List<List<Card>>> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            List<Card> shuffled = new ArrayList<>(cards);
            Collections.shuffle(shuffled, random);
            
            List<List<Card>> groups = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                List<Card> group = new ArrayList<>();
                for (int k = 0; k < 3; k++) {
                    group.add(shuffled.get(j * 3 + k));
                }
                groups.add(group);
            }
            
            // 去重
            String key = generateKey(groups);
            if (!seen.contains(key)) {
                seen.add(key);
                result.add(groups);
            }
        }
        
        return result;
    }
    
    /**
     * 生成组合的唯一键
     */
    private static String generateKey(List<List<Card>> groups) {
        List<String> groupKeys = new ArrayList<>();
        for (List<Card> group : groups) {
            List<Integer> values = new ArrayList<>();
            for (Card card : group) {
                values.add(card.getValue());
            }
            Collections.sort(values);
            groupKeys.add(values.toString());
        }
        Collections.sort(groupKeys);
        return groupKeys.toString();
    }
}

