package com.pokerai.training;

import com.pokerai.ai.AIPlayer;
import com.pokerai.simulator.GameSimulator;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 训练引擎 - 执行大量模拟训练
 */
public class TrainingEngine {
    
    private GameSimulator simulator;
    private AIPlayer aiPlayer;
    
    // 训练统计
    private AtomicLong totalGames = new AtomicLong(0);
    private AtomicLong aiWins = new AtomicLong(0);
    
    public TrainingEngine() {
        this.simulator = new GameSimulator();
        this.aiPlayer = simulator.getAIPlayer();
        
        // 启动时尝试加载历史经验库（如果存在）
        this.aiPlayer.loadExperience();
    }
    
    /**
     * 执行训练
     * @param numGames 训练游戏局数
     * @param reportInterval 报告间隔（每多少局报告一次）
     */
    public void train(long numGames, long reportInterval) {
        System.out.println("开始训练，总局数: " + numGames);
        System.out.println("初始探索率: " + String.format("%.2f%%", aiPlayer.getExplorationRate() * 100));
        System.out.println("初始经验库大小: " + aiPlayer.getExperienceSize());
        System.out.println("----------------------------------------");
        
        long startTime = System.currentTimeMillis();
        
        for (long i = 0; i < numGames; i++) {
            boolean aiWon = simulator.playOneGame();
            
            totalGames.incrementAndGet();
            if (aiWon) {
                aiWins.incrementAndGet();
            }
            
            // 定期降低探索率
            if (i > 0 && i % (numGames / 10) == 0) {
                aiPlayer.reduceExplorationRate(0.9);
            }
            
            // 定期报告进度
            if ((i + 1) % reportInterval == 0) {
                reportProgress(i + 1, numGames, startTime);
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 训练结束后持久化经验库
        this.aiPlayer.saveExperience();
        
        System.out.println("----------------------------------------");
        System.out.println("训练完成！");
        System.out.println("总游戏局数: " + totalGames.get());
        System.out.println("AI获胜次数: " + aiWins.get());
        System.out.println("AI胜率: " + String.format("%.2f%%", 
            (double) aiWins.get() / totalGames.get() * 100));
        System.out.println("最终探索率: " + String.format("%.2f%%", 
            aiPlayer.getExplorationRate() * 100));
        System.out.println("最终经验库大小: " + aiPlayer.getExperienceSize());
        System.out.println("总耗时: " + (duration / 1000.0) + " 秒");
        System.out.println("平均每局耗时: " + String.format("%.3f", duration / (double) numGames) + " 毫秒");
    }
    
    /**
     * 报告训练进度
     */
    private void reportProgress(long current, long total, long startTime) {
        double progress = (double) current / total * 100;
        long elapsed = System.currentTimeMillis() - startTime;
        double winRate = totalGames.get() > 0 ? 
            (double) aiWins.get() / totalGames.get() * 100 : 0;
        
        System.out.println(String.format(
            "进度: %d/%d (%.1f%%) | AI胜率: %.2f%% | 探索率: %.2f%% | 经验库: %d | 耗时: %.1f秒",
            current, total, progress, winRate, 
            aiPlayer.getExplorationRate() * 100,
            aiPlayer.getExperienceSize(),
            elapsed / 1000.0
        ));
    }
    
    /**
     * 获取AI玩家（用于测试和评估）
     */
    public AIPlayer getAIPlayer() {
        return aiPlayer;
    }
    
    /**
     * 获取训练统计
     */
    public TrainingStats getStats() {
        return new TrainingStats(
            totalGames.get(),
            aiWins.get(),
            aiPlayer.getExperienceSize(),
            aiPlayer.getExplorationRate()
        );
    }
    
    /**
     * 训练统计信息
     */
    public static class TrainingStats {
        private final long totalGames;
        private final long aiWins;
        private final int experienceSize;
        private final double explorationRate;
        
        public TrainingStats(long totalGames, long aiWins, int experienceSize, double explorationRate) {
            this.totalGames = totalGames;
            this.aiWins = aiWins;
            this.experienceSize = experienceSize;
            this.explorationRate = explorationRate;
        }
        
        public long getTotalGames() { return totalGames; }
        public long getAiWins() { return aiWins; }
        public int getExperienceSize() { return experienceSize; }
        public double getExplorationRate() { return explorationRate; }
        public double getWinRate() {
            return totalGames > 0 ? (double) aiWins / totalGames : 0;
        }
    }
}


