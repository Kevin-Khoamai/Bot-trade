package com.cryptotrading.apigateway.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TickerData {
    
    private String productId;
    private BigDecimal price;
    private BigDecimal bestBid;
    private BigDecimal bestAsk;
    private BigDecimal volume24h;
    private BigDecimal change24h;
    private BigDecimal changePercent24h;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String exchange;
    
    // Constructors
    public TickerData() {}
    
    public TickerData(String productId, BigDecimal price, BigDecimal bestBid, BigDecimal bestAsk, LocalDateTime timestamp) {
        this.productId = productId;
        this.price = price;
        this.bestBid = bestBid;
        this.bestAsk = bestAsk;
        this.timestamp = timestamp;
    }
    
    // Getters and Setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public BigDecimal getBestBid() { return bestBid; }
    public void setBestBid(BigDecimal bestBid) { this.bestBid = bestBid; }
    
    public BigDecimal getBestAsk() { return bestAsk; }
    public void setBestAsk(BigDecimal bestAsk) { this.bestAsk = bestAsk; }
    
    public BigDecimal getVolume24h() { return volume24h; }
    public void setVolume24h(BigDecimal volume24h) { this.volume24h = volume24h; }
    
    public BigDecimal getChange24h() { return change24h; }
    public void setChange24h(BigDecimal change24h) { this.change24h = change24h; }
    
    public BigDecimal getChangePercent24h() { return changePercent24h; }
    public void setChangePercent24h(BigDecimal changePercent24h) { this.changePercent24h = changePercent24h; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
    
    // Calculated properties
    public BigDecimal getSpread() {
        if (bestAsk != null && bestBid != null) {
            return bestAsk.subtract(bestBid);
        }
        return BigDecimal.ZERO;
    }
    
    public BigDecimal getSpreadPercent() {
        if (bestAsk != null && bestBid != null && price != null && price.compareTo(BigDecimal.ZERO) > 0) {
            return getSpread().divide(price, 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }
    
    @Override
    public String toString() {
        return "TickerData{" +
                "productId='" + productId + '\'' +
                ", price=" + price +
                ", bestBid=" + bestBid +
                ", bestAsk=" + bestAsk +
                ", timestamp=" + timestamp +
                ", exchange='" + exchange + '\'' +
                '}';
    }
}
