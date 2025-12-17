package com.pokerai.model;

/**
 * 扑克牌类
 */
public class Card {
    public enum Suit {
        SPADES("♠"), HEARTS("♥"), DIAMONDS("♦"), CLUBS("♣");
        
        private final String symbol;
        
        Suit(String symbol) {
            this.symbol = symbol;
        }
        
        public String getSymbol() {
            return symbol;
        }
    }
    
    public enum Rank {
        SIX(6, "6"), SEVEN(7, "7"), EIGHT(8, "8"), NINE(9, "9"), 
        TEN(10, "10"), JACK(11, "J"), QUEEN(12, "Q"), KING(13, "K"), ACE(14, "A");
        
        private final int value;
        private final String display;
        
        Rank(int value, String display) {
            this.value = value;
            this.display = display;
        }
        
        public int getValue() {
            return value;
        }
        
        public String getDisplay() {
            return display;
        }
    }
    
    public enum JokerType {
        NONE, SMALL, BIG
    }
    
    private final Suit suit;
    private final Rank rank;
    private final JokerType jokerType;
    
    public Card(Suit suit, Rank rank) {
        if (suit == null || rank == null) {
            throw new IllegalArgumentException("普通牌必须有花色和点数");
        }
        this.suit = suit;
        this.rank = rank;
        this.jokerType = JokerType.NONE;
    }
    
    private Card(JokerType jokerType) {
        if (jokerType == JokerType.NONE) {
            throw new IllegalArgumentException("万能牌类型不能为空");
        }
        this.suit = null;
        this.rank = null;
        this.jokerType = jokerType;
    }
    
    public static Card createSmallJoker() {
        return new Card(JokerType.SMALL);
    }
    
    public static Card createBigJoker() {
        return new Card(JokerType.BIG);
    }
    
    public boolean isJoker() {
        return jokerType != JokerType.NONE;
    }
    
    public JokerType getJokerType() {
        return jokerType;
    }
    
    public Suit getSuit() {
        return suit;
    }
    
    public Rank getRank() {
        return rank;
    }
    
    public int getValue() {
        return isJoker() ? 0 : rank.getValue();
    }
    
    @Override
    public String toString() {
        if (isJoker()) {
            return jokerType == JokerType.BIG ? "大王" : "小王";
        }
        return rank.getDisplay() + suit.getSymbol();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Card card = (Card) obj;
        if (isJoker() || card.isJoker()) {
            return jokerType == card.jokerType;
        }
        return suit == card.suit && rank == card.rank;
    }
    
    @Override
    public int hashCode() {
        if (isJoker()) {
            return jokerType.hashCode();
        }
        return suit.hashCode() * 31 + rank.hashCode();
    }
}


