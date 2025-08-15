package com.cryptotrading.apigateway.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "crypto_prices")
public class PriceData {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "symbol", nullable = false)
    private String symbol;
    
    @Column(name = "exchange", nullable = false)
    private String exchange;
    
    @Column(name = "price", nullable = false, precision = 20, scale = 8)
    private BigDecimal price;
    
    @Column(name = "volume", precision = 20, scale = 8)
    private BigDecimal volume;
    
    @Column(name = "timestamp", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    @Column(name = "interval_type")
    private String intervalType;
    
    @Column(name = "open_price", precision = 20, scale = 8)
    private BigDecimal openPrice;
    
    @Column(name = "high_price", precision = 20, scale = 8)
    private BigDecimal highPrice;
    
    @Column(name = "low_price", precision = 20, scale = 8)
    private BigDecimal lowPrice;
    
    @Column(name = "close_price", precision = 20, scale = 8)
    private BigDecimal closePrice;
    
    // Constructors
    public PriceData() {}
    
    public PriceData(String symbol, String exchange, BigDecimal price, LocalDateTime timestamp) {
        this.symbol = symbol;
        this.exchange = exchange;
        this.price = price;
        this.timestamp = timestamp;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public BigDecimal getVolume() { return volume; }
    public void setVolume(BigDecimal volume) { this.volume = volume; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getIntervalType() { return intervalType; }
    public void setIntervalType(String intervalType) { this.intervalType = intervalType; }
    
    public BigDecimal getOpenPrice() { return openPrice; }
    public void setOpenPrice(BigDecimal openPrice) { this.openPrice = openPrice; }
    
    public BigDecimal getHighPrice() { return highPrice; }
    public void setHighPrice(BigDecimal highPrice) { this.highPrice = highPrice; }
    
    public BigDecimal getLowPrice() { return lowPrice; }
    public void setLowPrice(BigDecimal lowPrice) { this.lowPrice = lowPrice; }
    
    public BigDecimal getClosePrice() { return closePrice; }
    public void setClosePrice(BigDecimal closePrice) { this.closePrice = closePrice; }
    
    @Override
    public String toString() {
        return "PriceData{" +
                "id=" + id +
                ", symbol='" + symbol + '\'' +
                ", exchange='" + exchange + '\'' +
                ", price=" + price +
                ", timestamp=" + timestamp +
                '}';
    }
}
