package com.pokerai.ai;

import com.pokerai.model.Card;
import com.pokerai.model.Player;
import com.pokerai.rules.GameRules;
import java.util.*;

/**
 * AI玩家 - 使用统计学习方法优化牌组合
 */
public class AIPlayer extends Player {
    
    // 经验库：存储不同牌型组合的胜率统计
    private Map<String, CombinationStats> experience;
    
    // 探索率：在训练过程中探索新组合的概率
    private double explorationRate;
    
    // 学习率：更新统计信息时的权重
    private double learningRate;
    
    public AIPlayer(int id, String name) {
        super(id, name);
        this.experience = new HashMap<>();
        this.explorationRate = 0.3; // 初始探索率30%
        this.learningRate = 0.1; // 学习率10%
    }
    
    /**
     * 组合统计信息
     */
    private static class CombinationStats {
        int wins = 0;
        int total = 0;
        double lastScore = 0;
        
        double getWinRate() {
            return total == 0 ? 0.0 : (double) wins / total;
        }
        
        void update(boolean won, double score) {
            total++;
            if (won) wins++;
            lastScore = score;
        }
    }
    
    /**
     * AI决策：将9张牌分成3组，每组3张
     * 这是核心AI逻辑
     */
    public List<List<Card>> decideCardGroups() {
        List<Card> hand = getHand();
        if (hand.size() != 9) {
            throw new IllegalStateException("手中必须有9张牌");
        }
        
        // 生成所有可能的分组方式（使用优化方法）
        // 由于完整组合数量巨大，使用随机采样方法
        List<List<List<Card>>> allCombinations = CombinationGenerator.generateRandomCombinations(hand, 500);
        
        // 评估每种组合
        List<CombinationEvaluation> evaluations = new ArrayList<>();
        for (List<List<Card>> combination : allCombinations) {
            List<List<Card>> ordered = GameRules.sortGroupsAscending(combination);
            double score = evaluateCombination(ordered);
            evaluations.add(new CombinationEvaluation(ordered, score));
        }
        
        // 根据探索率决定是选择最优还是随机探索
        if (Math.random() < explorationRate) {
            // 探索：随机选择一个组合
            Collections.shuffle(evaluations);
            return cloneGroups(evaluations.get(0).combination);
        } else {
            // 利用：选择得分最高的组合
            evaluations.sort((a, b) -> Double.compare(b.score, a.score));
            return cloneGroups(evaluations.get(0).combination);
        }
    }
    
    /**
     * 评估一个组合的得分
     */
    private double evaluateCombination(List<List<Card>> combination) {
        // 1. 计算基础得分（基于游戏规则）
        int baseScore = GameRules.evaluatePlayerGroups(combination);
        
        // 2. 查询经验库
        String key = generateCombinationKey(combination);
        CombinationStats stats = experience.get(key);
        
        double experienceScore = 0;
        if (stats != null) {
            // 结合历史胜率
            experienceScore = stats.getWinRate() * 1000;
        }
        
        // 3. 综合得分
        return baseScore * 0.7 + experienceScore * 0.3;
    }

    private List<List<Card>> cloneGroups(List<List<Card>> groups) {
        List<List<Card>> cloned = new ArrayList<>();
        for (List<Card> group : groups) {
            cloned.add(new ArrayList<>(group));
        }
        return cloned;
    }
    
    /**
     * 生成组合的唯一键（用于经验库）
     */
    private String generateCombinationKey(List<List<Card>> combination) {
        // 将组合转换为可比较的字符串
        List<String> groupKeys = new ArrayList<>();
        for (List<Card> group : combination) {
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
    
    /**
     * 学习：更新经验库
     */
    public void learn(List<List<Card>> combination, boolean won, double finalScore) {
        String key = generateCombinationKey(combination);
        CombinationStats stats = experience.get(key);
        
        if (stats == null) {
            stats = new CombinationStats();
            experience.put(key, stats);
        }
        
        stats.update(won, finalScore);
    }
    
    /**
     * 降低探索率（随着训练进行，减少探索，增加利用）
     */
    public void reduceExplorationRate(double factor) {
        explorationRate *= factor;
        if (explorationRate < 0.01) {
            explorationRate = 0.01; // 保持最小探索率
        }
    }
    
    public double getExplorationRate() {
        return explorationRate;
    }
    
    public int getExperienceSize() {
        return experience.size();
    }
    
    /**
     * 组合评估结果
     */
    private static class CombinationEvaluation {
        List<List<Card>> combination;
        double score;
        
        CombinationEvaluation(List<List<Card>> combination, double score) {
            this.combination = combination;
            this.score = score;
        }
    }
}

