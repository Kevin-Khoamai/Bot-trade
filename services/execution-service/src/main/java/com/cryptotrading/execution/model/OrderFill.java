package com.cryptotrading.execution.model;

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
 * Entity representing an order fill (partial or complete execution)
 */
@Entity
@Table(name = "order_fills")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderFill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "exchange_fill_id", nullable = false)
    private String exchangeFillId;

    @Column(name = "exchange_trade_id")
    private String exchangeTradeId;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "exchange", nullable = false)
    private String exchange;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false)
    private Order.OrderSide side;

    // Fill Details
    @Column(name = "quantity", precision = 20, scale = 8, nullable = false)
    private BigDecimal quantity;

    @Column(name = "price", precision = 20, scale = 8, nullable = false)
    private BigDecimal price;

    @Column(name = "fee", precision = 20, scale = 8)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "fee_currency", length = 10)
    private String feeCurrency;

    @Column(name = "commission_rate", precision = 10, scale = 6)
    private BigDecimal commissionRate;

    // Market Context
    @Column(name = "market_price", precision = 20, scale = 8)
    private BigDecimal marketPrice;

    @Column(name = "bid_price", precision = 20, scale = 8)
    private BigDecimal bidPrice;

    @Column(name = "ask_price", precision = 20, scale = 8)
    private BigDecimal askPrice;

    @Column(name = "spread", precision = 20, scale = 8)
    private BigDecimal spread;

    // Execution Quality Metrics
    @Column(name = "price_improvement", precision = 20, scale = 8)
    private BigDecimal priceImprovement;

    @Column(name = "slippage", precision = 20, scale = 8)
    private BigDecimal slippage;

    @Column(name = "execution_latency_ms")
    private Long executionLatencyMs;

    // Liquidity Information
    @Enumerated(EnumType.STRING)
    @Column(name = "liquidity_type")
    private LiquidityType liquidityType;

    @Column(name = "maker_rebate", precision = 20, scale = 8)
    private BigDecimal makerRebate;

    // Timestamps
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Additional Context
    @Column(name = "counterparty_id")
    private String counterpartyId;

    @Column(name = "venue")
    private String venue;

    @Column(name = "execution_venue")
    private String executionVenue;

    /**
     * Liquidity types
     */
    public enum LiquidityType {
        MAKER,      // Added liquidity to order book
        TAKER,      // Removed liquidity from order book
        UNKNOWN     // Liquidity type not determined
    }

    /**
     * Calculate total value of the fill
     */
    public BigDecimal getTotalValue() {
        return quantity.multiply(price);
    }

    /**
     * Calculate net value after fees
     */
    public BigDecimal getNetValue() {
        BigDecimal totalValue = getTotalValue();
        BigDecimal totalFee = fee != null ? fee : BigDecimal.ZERO;
        
        // For buy orders, fees are added to cost
        // For sell orders, fees are subtracted from proceeds
        return side == Order.OrderSide.BUY 
            ? totalValue.add(totalFee)
            : totalValue.subtract(totalFee);
    }

    /**
     * Calculate price improvement (positive = better than expected)
     */
    public BigDecimal calculatePriceImprovement(BigDecimal expectedPrice) {
        if (expectedPrice == null) {
            return BigDecimal.ZERO;
        }
        
        return side == Order.OrderSide.BUY 
            ? expectedPrice.subtract(price)  // Lower price is better for buy
            : price.subtract(expectedPrice); // Higher price is better for sell
    }

    /**
     * Calculate slippage (positive = worse than expected)
     */
    public BigDecimal calculateSlippage(BigDecimal expectedPrice) {
        if (expectedPrice == null) {
            return BigDecimal.ZERO;
        }
        
        return side == Order.OrderSide.BUY 
            ? price.subtract(expectedPrice)  // Higher price is worse for buy
            : expectedPrice.subtract(price); // Lower price is worse for sell
    }

    /**
     * Calculate effective spread at time of execution
     */
    public BigDecimal calculateEffectiveSpread() {
        if (bidPrice == null || askPrice == null) {
            return BigDecimal.ZERO;
        }
        return askPrice.subtract(bidPrice);
    }

    /**
     * Calculate spread as percentage of mid price
     */
    public BigDecimal calculateSpreadPercentage() {
        if (bidPrice == null || askPrice == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal midPrice = bidPrice.add(askPrice).divide(BigDecimal.valueOf(2), 8, java.math.RoundingMode.HALF_UP);
        BigDecimal spread = calculateEffectiveSpread();
        
        return midPrice.compareTo(BigDecimal.ZERO) == 0 
            ? BigDecimal.ZERO 
            : spread.divide(midPrice, 6, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    /**
     * Check if this fill provided liquidity (maker)
     */
    public boolean isMaker() {
        return liquidityType == LiquidityType.MAKER;
    }

    /**
     * Check if this fill removed liquidity (taker)
     */
    public boolean isTaker() {
        return liquidityType == LiquidityType.TAKER;
    }

    /**
     * Calculate execution quality score (0-100)
     */
    public BigDecimal calculateExecutionQualityScore() {
        BigDecimal score = BigDecimal.valueOf(50); // Base score
        
        // Adjust for price improvement
        if (priceImprovement != null && priceImprovement.compareTo(BigDecimal.ZERO) > 0) {
            score = score.add(BigDecimal.valueOf(20));
        }
        
        // Adjust for slippage
        if (slippage != null && slippage.compareTo(BigDecimal.ZERO) > 0) {
            score = score.subtract(BigDecimal.valueOf(15));
        }
        
        // Adjust for liquidity provision
        if (isMaker()) {
            score = score.add(BigDecimal.valueOf(10));
        }
        
        // Adjust for execution speed
        if (executionLatencyMs != null && executionLatencyMs < 100) {
            score = score.add(BigDecimal.valueOf(10));
        } else if (executionLatencyMs != null && executionLatencyMs > 1000) {
            score = score.subtract(BigDecimal.valueOf(10));
        }
        
        // Ensure score is between 0 and 100
        return score.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100));
    }

    /**
     * Get fee rate as percentage
     */
    public BigDecimal getFeeRatePercentage() {
        if (commissionRate == null) {
            return BigDecimal.ZERO;
        }
        return commissionRate.multiply(BigDecimal.valueOf(100));
    }

    /**
     * Check if fill was executed at better than market price
     */
    public boolean isBetterThanMarket() {
        if (marketPrice == null) {
            return false;
        }
        
        return side == Order.OrderSide.BUY 
            ? price.compareTo(marketPrice) < 0  // Bought below market
            : price.compareTo(marketPrice) > 0; // Sold above market
    }

    /**
     * Calculate market impact (how much the fill moved the market)
     */
    public BigDecimal calculateMarketImpact(BigDecimal priceAfterFill) {
        if (marketPrice == null || priceAfterFill == null) {
            return BigDecimal.ZERO;
        }
        
        return priceAfterFill.subtract(marketPrice).abs()
                .divide(marketPrice, 6, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
