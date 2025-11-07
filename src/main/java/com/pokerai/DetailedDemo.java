package com.pokerai;

import com.pokerai.simulator.GameResult;
import com.pokerai.simulator.GameSimulator;

/**
 * 单局详细演示：使用新规则（38张牌，大小王万能，去掉2-5），
 * 4名玩家随机发牌，玩家1作为主视角，9张牌分3组，
 * 每道按从大到小排序后比较并计分：最大+12分，其余依次-2、-4、-6。
 * 输出每道的对比记录、得分明细及4名玩家最终得分汇总。
 */
public class DetailedDemo {

    public static void main(String[] args) {
        GameSimulator simulator = new GameSimulator();
        GameResult result = simulator.playOneGameWithDetails();
        if (result == null) {
            System.out.println("本次演示未产生有效结果。");
        }
    }
}


