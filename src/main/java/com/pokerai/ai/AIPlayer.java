package com.pokerai.ai;

import com.pokerai.model.Card;
import com.pokerai.model.Player;
import com.pokerai.rules.BonusRules;
import com.pokerai.rules.GameRules;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI玩家 - 使用统计学习方法优化牌组合
 */
public class AIPlayer extends Player {
    
    // 经验库：存储不同牌型模式的历史表现（胜率 + 平均收益）
    private Map<String, CombinationStats> experience;
    // 默认经验库持久化文件
    private static final String DEFAULT_EXPERIENCE_FILE = "ai_experience.dat";
    
    
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
     * 从默认文件加载经验库
     * 如果文件不存在或读取失败，则忽略错误，使用空经验库。
     */
    public synchronized void loadExperience() {
        loadExperience(DEFAULT_EXPERIENCE_FILE);
    }

    /**
     * 从指定文件加载经验库
     */
    @SuppressWarnings("unchecked")
    public synchronized void loadExperience(String filePath) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            Object obj = ois.readObject();
            if (obj instanceof Map) {
                this.experience = (Map<String, CombinationStats>) obj;
                System.out.println("[AI] 经验库已加载，条目数: " + experience.size());
            }
        } catch (IOException | ClassNotFoundException e) {
            // 文件不存在或无法读取时，不影响程序运行
            System.out.println("[AI] 未找到经验库文件或加载失败，使用空经验库: " + e.getMessage());
        }
    }

    /**
     * 将经验库保存到默认文件
     */
    public synchronized void saveExperience() {
        saveExperience(DEFAULT_EXPERIENCE_FILE);
    }

    /**
     * 将经验库保存到指定文件
     */
    public synchronized void saveExperience(String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(this.experience);
            System.out.println("[AI] 经验库已保存，条目数: " + experience.size());
        } catch (IOException e) {
            System.out.println("[AI] 保存经验库失败: " + e.getMessage());
        }
    }

    /**
     * 组合统计信息
     * 负责记录某一种配牌模式在历史对局中的表现：
     * - 使用次数 total
     * - 获胜次数 wins
     * - 累计总收益 sumScore（基础分 + 喜钱 + 通关）
     *
     * 由此可以得到：
     * - 胜率 winRate
     * - 平均收益 avgScore（期望总收益）
     */
    private static class CombinationStats implements Serializable {
        private static final long serialVersionUID = 1L;
        int wins = 0;
        int total = 0;
        double sumScore = 0;
        
        /**
         * 胜率 = wins / total
         */
        double getWinRate() {
            return total == 0 ? 0.0 : (double) wins / total;
        }
        
        /**
         * 平均收益 = 历史总得分 / 使用次数
         */
        double getAvgScore() {
            return total == 0 ? 0.0 : sumScore / total;
        }
        
        /**
         * 更新统计信息
         * @param won        本局是否获胜
         * @param finalScore 本局最终总得分（基础分 + 喜钱 + 通关奖励）
         */
        void update(boolean won, double finalScore) {
            total++;
            if (won) {
                wins++;
            }
            sumScore += finalScore;
        }
    }

    // AI决策：将9张牌分成3组，每组3张（穷举所有分组，选最优）
    public List<List<Card>> decideCardGroups() {
        List<Card> hand = getHand();
        if (hand.size() != 9) {
            throw new IllegalStateException("手中必须有9张牌");
        }

        // 1. 枚举所有可能的 3×3×3 分组（最多 280 种）
        List<List<List<Card>>> allCombinations = CombinationGenerator.generateAllCombinations(hand);

        // 2. 对每种分组，先按照 强度 从弱到强 排好三道，然后打总分
        List<CombinationEvaluation> evaluations = new ArrayList<>();
        for (List<List<Card>> rawCombination : allCombinations) {
            // 按牌型强度排序：0 = 头道最弱，2 = 尾道最强
            List<List<Card>> ordered = GameRules.sortGroupsAscending(rawCombination);

            // 计算这一种配牌的基础得分
            int baseScore = GameRules.evaluatePlayerGroups(ordered);

            // 3. 喜钱：基于当前组合 + 整副手牌计算
            BonusRules.BonusResult bonusResult = BonusRules.checkPlayerBonus(
                    ordered,
                    getHand() // AI 的完整9张牌
            );
            int bonusScore = (bonusResult != null) ? bonusResult.getBonus() : 0;

            // 叠加经验：从经验库中取出该配牌模式的历史表现
            String key = generateCombinationKey(ordered);
            CombinationStats stats = experience.get(key);
            double winRate      = stats == null ? 0.0 : stats.getWinRate();
            double avgScoreHist = stats == null ? 0.0 : stats.getAvgScore();

            // 优化打分公式：
            //  - 喜钱：决定这局“能不能爆发”
            //  - 历史平均收益：这类配法长期“平均能赚多少分”（你关心的核心指标）
            //  - 胜率：作为辅助参考
            //  - 基础分：在没有喜钱或经验不足时兜底
            double totalScore;
            if (bonusScore > 0) {
                // 有喜钱时：优先高喜钱 + 高历史平均收益
                totalScore =
                        baseScore    * 0.1   +   // 当前这局的裸牌力影响很小
                        bonusScore   * 10000 +   // 喜钱权重极高
                        avgScoreHist * 50.0  +   // 这类配法历史平均收益
                        winRate      * 1000.0;   // 胜率作为辅助
            } else {
                // 无喜钱时：主要看基础分 + 历史平均收益 + 胜率
                totalScore =
                        baseScore    * 1.0   +   // 当前牌力
                        avgScoreHist * 10.0  +   // 历史平均收益
                        winRate      * 500.0;    // 胜率
            }
            
            evaluations.add(new CombinationEvaluation(ordered, totalScore, baseScore, bonusScore));
        }

        // 3. 按评分从高到低排序
        evaluations.sort((a, b) -> {
            // 首先按总分排序
            int scoreCompare = Double.compare(b.score, a.score);
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            // 如果总分相同，优先选择喜钱高的
            int bonusCompare = Integer.compare(b.bonusScore, a.bonusScore);
            if (bonusCompare != 0) {
                return bonusCompare;
            }
            // 如果喜钱也相同，选择基础分高的
            return Integer.compare(b.baseScore, a.baseScore);
        });

        // 4. 加一点"探索"逻辑：大多数时候选最优，小概率随机从前若干名里挑一个
        List<CombinationEvaluation> topList = evaluations;
        int topN = Math.min(10, evaluations.size()); // 只在前 10 个里探索（减少探索范围）
        if (topN > 0) {
            topList = evaluations.subList(0, topN);
        }

        CombinationEvaluation chosen;
        if (Math.random() < explorationRate && topList.size() > 1) {
            // 探索：在前 topN 名里随机选一个
            Collections.shuffle(topList);
            chosen = topList.get(0);
        } else {
            // 利用：选当前评分最高的那个
            chosen = evaluations.get(0);
        }

        // 返回一个防御性拷贝
        return cloneGroups(chosen.combination);
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
     *
     * 升级为“模式经验”：
     *  - 不再直接使用具体点数组合，而是使用每一道的牌型模式
     *  - 例如：HEAD:STRAIGHT_J / MID:FLUSH / TAIL:THREE_OF_A_KIND_J
     * 这样结构相似的配牌（如不同点数的双三条 + 顺子 + 九顺子）可以共享经验。
     */
    private String generateCombinationKey(List<List<Card>> combination) {
        List<String> groupPatterns = new ArrayList<>();
        
        for (List<Card> group : combination) {
            // 使用 GameRules 来获取这道牌的牌型
            GameRules.HandResult result = GameRules.evaluateHand(group);
            GameRules.HandType type = result.getHandType();
            
            // 标记这一道是否使用了万能牌
            boolean hasJoker = false;
            for (Card card : group) {
                if (card.isJoker()) {
                    hasJoker = true;
                    break;
                }
            }
            
            // 构造模式字符串：例如 "THREE_OF_A_KIND_J" / "FLUSH" / "STRAIGHT_J"
            StringBuilder sb = new StringBuilder();
            sb.append(type.name());
            if (hasJoker) {
                sb.append("_J");
            }
            groupPatterns.add(sb.toString());
        }
        
        // 为了忽略“头中尾顺序”的差异，先排序后再拼成 key，
        // 真正的头中尾关系已经由 sortGroupsAscending 保证，这里只学习整体模式。
        Collections.sort(groupPatterns);
        return groupPatterns.toString();
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
        int baseScore;      // 基础分（用于调试和排序）
        int bonusScore;     // 喜钱（用于调试和排序）
        
        CombinationEvaluation(List<List<Card>> combination, double score, int baseScore, int bonusScore) {
            this.combination = combination;
            this.score = score;
            this.baseScore = baseScore;
            this.bonusScore = bonusScore;
        }
    }
}


