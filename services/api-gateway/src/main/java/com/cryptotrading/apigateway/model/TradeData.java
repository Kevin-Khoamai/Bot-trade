package com.cryptotrading.apigateway.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TradeData {
    
    private String productId;
    private BigDecimal price;
    private BigDecimal size;
    private String side; // "buy" or "sell"
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String exchange;
    private String tradeId;
    
    // Constructors
    public TradeData() {}
    
    public TradeData(String productId, BigDecimal price, BigDecimal size, String side, LocalDateTime timestamp) {
        this.productId = productId;
        this.price = price;
        this.size = size;
        this.side = side;
        this.timestamp = timestamp;
    }
    
    // Getters and Setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public BigDecimal getSize() { return size; }
    public void setSize(BigDecimal size) { this.size = size; }
    
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
    
    public String getTradeId() { return tradeId; }
    public void setTradeId(String tradeId) { this.tradeId = tradeId; }
    
    @Override
    public String toString() {
        return "TradeData{" +
                "productId='" + productId + '\'' +
                ", price=" + price +
                ", size=" + size +
                ", side='" + side + '\'' +
                ", timestamp=" + timestamp +
                ", exchange='" + exchange + '\'' +
                '}';
    }
}
