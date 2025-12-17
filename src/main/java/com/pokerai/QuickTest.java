package com.pokerai;

import com.pokerai.training.TrainingEngine;

/**
 * 快速测试类 - 用于快速验证系统是否正常工作
 */
public class QuickTest {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("    快速测试 - 100局游戏");
        System.out.println("========================================");
        System.out.println();
        
        // 快速测试：100局游戏
        TrainingEngine engine = new TrainingEngine();
        engine.train(100, 25);
        
        System.out.println();
        System.out.println("测试完成！如果看到以上输出，说明系统运行正常。");
        System.out.println("可以运行 Main.java 进行正式训练。");
    }
}


