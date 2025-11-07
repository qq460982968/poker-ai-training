package com.pokerai.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 牌堆类 - 管理一副完整的扑克牌
 */
public class Deck {
    private List<Card> cards;
    
    public Deck() {
        initializeDeck();
    }
    
    /**
     * 初始化一副完整的52张牌
     */
    private void initializeDeck() {
        cards = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
        cards.add(Card.createSmallJoker());
        cards.add(Card.createBigJoker());
    }
    
    /**
     * 洗牌
     */
    public void shuffle() {
        Collections.shuffle(cards);
    }
    
    /**
     * 发牌
     * @param count 发牌数量
     * @return 发出去的牌
     */
    public List<Card> deal(int count) {
        if (cards.size() < count) {
            throw new IllegalStateException("牌堆中的牌不够了");
        }
        
        List<Card> dealt = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            dealt.add(cards.remove(0));
        }
        return dealt;
    }
    
    /**
     * 获取剩余牌数
     */
    public int remainingCards() {
        return cards.size();
    }
    
    /**
     * 重置牌堆（重新初始化并洗牌）
     */
    public void reset() {
        initializeDeck();
        shuffle();
    }
    
    /**
     * 检查是否还有足够的牌
     */
    public boolean hasEnoughCards(int count) {
        return cards.size() >= count;
    }
}

