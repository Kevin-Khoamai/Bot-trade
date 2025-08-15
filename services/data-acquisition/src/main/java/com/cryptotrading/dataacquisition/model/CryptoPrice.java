package com.cryptotrading.dataacquisition.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing cryptocurrency price data (OHLCV)
 */
@Entity
@Table(name = "crypto_prices", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"exchange", "symbol", "timestamp"}),
       indexes = {
           @Index(name = "idx_symbol_timestamp", columnList = "symbol, timestamp"),
           @Index(name = "idx_exchange_symbol", columnList = "exchange, symbol")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CryptoPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String exchange;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "open_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal openPrice;

    @Column(name = "high_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal highPrice;

    @Column(name = "low_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal lowPrice;

    @Column(name = "close_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal closePrice;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal volume;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Create a CryptoPrice from market data
     */
    public static CryptoPrice fromMarketData(String exchange, String symbol, 
                                           LocalDateTime timestamp, BigDecimal open, 
                                           BigDecimal high, BigDecimal low, 
                                           BigDecimal close, BigDecimal volume) {
        return CryptoPrice.builder()
                .exchange(exchange)
                .symbol(symbol)
                .timestamp(timestamp)
                .openPrice(open)
                .highPrice(high)
                .lowPrice(low)
                .closePrice(close)
                .volume(volume)
                .build();
    }

    /**
     * Get the price change percentage
     */
    public BigDecimal getPriceChangePercent() {
        if (openPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return closePrice.subtract(openPrice)
                .divide(openPrice, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Check if this is a valid price data
     */
    public boolean isValid() {
        return openPrice != null && openPrice.compareTo(BigDecimal.ZERO) > 0 &&
               highPrice != null && highPrice.compareTo(BigDecimal.ZERO) > 0 &&
               lowPrice != null && lowPrice.compareTo(BigDecimal.ZERO) > 0 &&
               closePrice != null && closePrice.compareTo(BigDecimal.ZERO) > 0 &&
               volume != null && volume.compareTo(BigDecimal.ZERO) >= 0 &&
               highPrice.compareTo(lowPrice) >= 0 &&
               highPrice.compareTo(openPrice) >= 0 &&
               highPrice.compareTo(closePrice) >= 0 &&
               lowPrice.compareTo(openPrice) <= 0 &&
               lowPrice.compareTo(closePrice) <= 0;
    }
}
