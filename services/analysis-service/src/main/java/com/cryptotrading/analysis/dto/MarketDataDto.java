package com.cryptotrading.analysis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for market data consumed from Kafka
 * This should match the MarketDataDto from data-acquisition service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDataDto {

    private String exchange;
    private String symbol;
    private LocalDateTime timestamp;
    
    @JsonProperty("open")
    private BigDecimal openPrice;
    
    @JsonProperty("high")
    private BigDecimal highPrice;
    
    @JsonProperty("low")
    private BigDecimal lowPrice;
    
    @JsonProperty("close")
    private BigDecimal closePrice;
    
    private BigDecimal volume;
    private String dataType; // KLINE, TRADE, TICKER
    private String source; // REST, WEBSOCKET

    /**
     * Get typical price (HLC/3)
     */
    public BigDecimal getTypicalPrice() {
        if (highPrice == null || lowPrice == null || closePrice == null) {
            return closePrice;
        }
        return highPrice.add(lowPrice).add(closePrice)
                .divide(BigDecimal.valueOf(3), 8, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Get price change from open to close
     */
    public BigDecimal getPriceChange() {
        if (openPrice == null || closePrice == null) {
            return BigDecimal.ZERO;
        }
        return closePrice.subtract(openPrice);
    }

    /**
     * Get price change percentage
     */
    public BigDecimal getPriceChangePercent() {
        if (openPrice == null || closePrice == null || openPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getPriceChange()
                .divide(openPrice, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Get true range for ATR calculation
     */
    public BigDecimal getTrueRange(BigDecimal previousClose) {
        if (highPrice == null || lowPrice == null || closePrice == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal hl = highPrice.subtract(lowPrice);
        
        if (previousClose == null) {
            return hl;
        }
        
        BigDecimal hc = highPrice.subtract(previousClose).abs();
        BigDecimal lc = lowPrice.subtract(previousClose).abs();
        
        return hl.max(hc).max(lc);
    }

    /**
     * Check if this is valid market data for analysis
     */
    public boolean isValidForAnalysis() {
        return symbol != null && !symbol.trim().isEmpty() &&
               timestamp != null &&
               closePrice != null && closePrice.compareTo(BigDecimal.ZERO) > 0 &&
               volume != null && volume.compareTo(BigDecimal.ZERO) >= 0;
    }
}
