package com.pokerai.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家类
 */
public class Player {
    private final int id;
    private final String name;
    private List<Card> hand; // 手中的9张牌
    private List<List<Card>> cardGroups; // 组合后的牌组（例如：3+3+3的组合）
    private int wins; // 获胜次数
    private int games; // 参与游戏次数
    private boolean surrendered; // 是否投降
    
    public Player(int id, String name) {
        this.id = id;
        this.name = name;
        this.hand = new ArrayList<>();
        this.cardGroups = new ArrayList<>();
        this.wins = 0;
        this.games = 0;
        this.surrendered = false;
    }
    
    /**
     * 发牌给玩家
     */
    public void receiveCards(List<Card> cards) {
        this.hand = new ArrayList<>(cards);
        this.cardGroups = new ArrayList<>();
    }
    
    /**
     * 设置牌的组合方式
     */
    public void setCardGroups(List<List<Card>> groups) {
        this.cardGroups = new ArrayList<>(groups);
    }
    
    public List<Card> getHand() {
        return new ArrayList<>(hand);
    }
    
    public List<List<Card>> getCardGroups() {
        return new ArrayList<>(cardGroups);
    }
    
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void addWin() {
        wins++;
        games++;
    }
    
    public void addGame() {
        games++;
    }
    
    public int getWins() {
        return wins;
    }
    
    public int getGames() {
        return games;
    }
    
    public double getWinRate() {
        return games == 0 ? 0.0 : (double) wins / games;
    }
    
    /**
     * 设置投降状态
     */
    public void setSurrendered(boolean surrendered) {
        this.surrendered = surrendered;
    }
    
    /**
     * 是否投降
     */
    public boolean isSurrendered() {
        return surrendered;
    }
    
    @Override
    public String toString() {
        return name + " (ID: " + id + ", 胜率: " + String.format("%.2f%%", getWinRate() * 100) + ")";
    }
}


