package com.pokerai;

import com.pokerai.training.TrainingEngine;
import com.pokerai.simulator.GameSimulator;
import java.util.Scanner;

/**
 * 主程序入口
 */
public class Main {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("    扑克牌AI训练系统");
        System.out.println("    新规则：38张牌（6-A），大小王万能，4人每人9张，3x3x3");
        System.out.println("========================================");
        System.out.println();
        
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("请选择模式：");
        System.out.println("  1) 单局详细演示（输出每道比牌记录与得分汇总）");
        System.out.println("  2) 训练模式（批量对局，统计AI胜率）");
        System.out.print("请输入选项(1/2): ");
        int choice = scanner.nextInt();
        
        if (choice == 1) {
            System.out.println();
            System.out.println("开始单局详细演示...");
            GameSimulator simulator = new GameSimulator();
            simulator.playOneGameWithDetails();
        } else {
            // 获取训练参数
            System.out.print("请输入训练局数（建议10000-100000）: ");
            long numGames = scanner.nextLong();
            
            System.out.print("请输入报告间隔（每多少局报告一次，建议1000）: ");
            long reportInterval = scanner.nextLong();
            
            System.out.println();
            System.out.println("开始训练...");
            System.out.println();
            
            // 创建训练引擎并开始训练
            TrainingEngine engine = new TrainingEngine();
            engine.train(numGames, reportInterval);
            
            // 显示最终统计
            System.out.println();
            System.out.println("========================================");
            System.out.println("训练统计信息：");
            TrainingEngine.TrainingStats stats = engine.getStats();
            System.out.println("  总游戏局数: " + stats.getTotalGames());
            System.out.println("  AI获胜次数: " + stats.getAiWins());
            System.out.println("  AI胜率: " + String.format("%.2f%%", stats.getWinRate() * 100));
            System.out.println("  经验库大小: " + stats.getExperienceSize());
            System.out.println("  最终探索率: " + String.format("%.2f%%", stats.getExplorationRate() * 100));
            System.out.println("========================================");
        }
        
        scanner.close();
    }
}


