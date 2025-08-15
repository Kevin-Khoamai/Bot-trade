package com.cryptotrading.portfolio.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a trading position
 */
@Entity
@Table(name = "positions", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"portfolio_id", "symbol", "exchange"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "exchange", nullable = false)
    private String exchange;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false)
    private PositionSide side;

    // Quantity Information
    @Column(name = "quantity", precision = 20, scale = 8, nullable = false)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "available_quantity", precision = 20, scale = 8)
    private BigDecimal availableQuantity = BigDecimal.ZERO;

    @Column(name = "locked_quantity", precision = 20, scale = 8)
    private BigDecimal lockedQuantity = BigDecimal.ZERO;

    // Cost Basis Information
    @Column(name = "average_cost", precision = 20, scale = 8)
    private BigDecimal averageCost = BigDecimal.ZERO;

    @Column(name = "total_cost", precision = 20, scale = 8)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(name = "total_fees", precision = 20, scale = 8)
    private BigDecimal totalFees = BigDecimal.ZERO;

    // Market Value Information
    @Column(name = "current_price", precision = 20, scale = 8)
    private BigDecimal currentPrice;

    @Column(name = "market_value", precision = 20, scale = 8)
    private BigDecimal marketValue;

    @Column(name = "last_price_update")
    private LocalDateTime lastPriceUpdate;

    // P&L Information
    @Column(name = "unrealized_pnl", precision = 20, scale = 8)
    private BigDecimal unrealizedPnl = BigDecimal.ZERO;

    @Column(name = "realized_pnl", precision = 20, scale = 8)
    private BigDecimal realizedPnl = BigDecimal.ZERO;

    @Column(name = "total_pnl", precision = 20, scale = 8)
    private BigDecimal totalPnl = BigDecimal.ZERO;

    @Column(name = "pnl_percentage", precision = 10, scale = 4)
    private BigDecimal pnlPercentage = BigDecimal.ZERO;

    // Risk Metrics
    @Column(name = "daily_pnl", precision = 20, scale = 8)
    private BigDecimal dailyPnl = BigDecimal.ZERO;

    @Column(name = "max_drawdown", precision = 10, scale = 4)
    private BigDecimal maxDrawdown = BigDecimal.ZERO;

    @Column(name = "high_water_mark", precision = 20, scale = 8)
    private BigDecimal highWaterMark = BigDecimal.ZERO;

    @Column(name = "var_95", precision = 20, scale = 8)
    private BigDecimal var95;

    @Column(name = "volatility", precision = 10, scale = 6)
    private BigDecimal volatility;

    // Position Metadata
    @Column(name = "first_trade_date")
    private LocalDateTime firstTradeDate;

    @Column(name = "last_trade_date")
    private LocalDateTime lastTradeDate;

    @Column(name = "trade_count")
    private Integer tradeCount = 0;

    @Column(name = "winning_trades")
    private Integer winningTrades = 0;

    @Column(name = "losing_trades")
    private Integer losingTrades = 0;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    /**
     * Position sides
     */
    public enum PositionSide {
        LONG,   // Holding the asset
        SHORT,  // Short selling the asset
        FLAT    // No position
    }

    /**
     * Update position with a new trade
     */
    public void updateWithTrade(BigDecimal tradeQuantity, BigDecimal tradePrice, BigDecimal tradeFee, LocalDateTime tradeTime) {
        // Update trade statistics
        this.tradeCount++;
        this.lastTradeDate = tradeTime;
        if (this.firstTradeDate == null) {
            this.firstTradeDate = tradeTime;
        }

        // Calculate new average cost and total cost
        BigDecimal newTotalCost = this.totalCost.add(tradeQuantity.multiply(tradePrice));
        BigDecimal newTotalQuantity = this.quantity.add(tradeQuantity);
        
        if (newTotalQuantity.compareTo(BigDecimal.ZERO) != 0) {
            this.averageCost = newTotalCost.divide(newTotalQuantity, 8, RoundingMode.HALF_UP);
        }

        // Update quantities
        this.quantity = newTotalQuantity;
        this.availableQuantity = this.quantity.subtract(this.lockedQuantity);
        this.totalCost = newTotalCost;
        this.totalFees = this.totalFees.add(tradeFee);

        // Determine position side
        if (this.quantity.compareTo(BigDecimal.ZERO) > 0) {
            this.side = PositionSide.LONG;
        } else if (this.quantity.compareTo(BigDecimal.ZERO) < 0) {
            this.side = PositionSide.SHORT;
        } else {
            this.side = PositionSide.FLAT;
        }

        // Update P&L if current price is available
        if (this.currentPrice != null) {
            updatePnL();
        }
    }

    /**
     * Update P&L calculations
     */
    public void updatePnL() {
        if (currentPrice == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        // Calculate market value
        this.marketValue = quantity.multiply(currentPrice);

        // Calculate unrealized P&L
        this.unrealizedPnl = marketValue.subtract(totalCost);

        // Calculate total P&L
        this.totalPnl = realizedPnl.add(unrealizedPnl);

        // Calculate P&L percentage
        if (totalCost.compareTo(BigDecimal.ZERO) != 0) {
            this.pnlPercentage = totalPnl.divide(totalCost.abs(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // Update high water mark and drawdown
        updateDrawdown();
    }

    /**
     * Update price and recalculate P&L
     */
    public void updatePrice(BigDecimal newPrice, LocalDateTime priceTime) {
        BigDecimal previousValue = this.marketValue;
        
        this.currentPrice = newPrice;
        this.lastPriceUpdate = priceTime;
        
        updatePnL();
        
        // Calculate daily P&L if we have previous value
        if (previousValue != null) {
            this.dailyPnl = this.marketValue.subtract(previousValue);
        }
    }

    /**
     * Update drawdown calculations
     */
    private void updateDrawdown() {
        if (totalPnl.compareTo(highWaterMark) > 0) {
            highWaterMark = totalPnl;
        }

        if (highWaterMark.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentDrawdown = highWaterMark.subtract(totalPnl)
                    .divide(highWaterMark, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            
            if (currentDrawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = currentDrawdown;
            }
        }
    }

    /**
     * Close position
     */
    public void closePosition(BigDecimal closePrice, BigDecimal closeFee, LocalDateTime closeTime) {
        // Calculate final realized P&L
        BigDecimal finalValue = quantity.multiply(closePrice).subtract(closeFee);
        this.realizedPnl = this.realizedPnl.add(finalValue.subtract(totalCost));
        
        // Reset position
        this.quantity = BigDecimal.ZERO;
        this.availableQuantity = BigDecimal.ZERO;
        this.marketValue = BigDecimal.ZERO;
        this.unrealizedPnl = BigDecimal.ZERO;
        this.totalPnl = this.realizedPnl;
        this.side = PositionSide.FLAT;
        this.closedAt = closeTime;
        this.lastTradeDate = closeTime;
    }

    /**
     * Lock quantity for pending orders
     */
    public void lockQuantity(BigDecimal quantityToLock) {
        if (quantityToLock.compareTo(availableQuantity) > 0) {
            throw new IllegalArgumentException("Cannot lock more than available quantity");
        }
        this.lockedQuantity = this.lockedQuantity.add(quantityToLock);
        this.availableQuantity = this.availableQuantity.subtract(quantityToLock);
    }

    /**
     * Unlock quantity when orders are cancelled or filled
     */
    public void unlockQuantity(BigDecimal quantityToUnlock) {
        this.lockedQuantity = this.lockedQuantity.subtract(quantityToUnlock);
        this.availableQuantity = this.availableQuantity.add(quantityToUnlock);
    }

    /**
     * Check if position is open
     */
    public boolean isOpen() {
        return quantity.compareTo(BigDecimal.ZERO) != 0 && side != PositionSide.FLAT;
    }

    /**
     * Check if position is profitable
     */
    public boolean isProfitable() {
        return totalPnl.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Get win rate
     */
    public BigDecimal getWinRate() {
        if (tradeCount == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(winningTrades)
                .divide(BigDecimal.valueOf(tradeCount), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Get position value in base currency
     */
    public BigDecimal getPositionValue() {
        return marketValue != null ? marketValue : BigDecimal.ZERO;
    }

    /**
     * Get position weight in portfolio
     */
    public BigDecimal getPositionWeight(BigDecimal portfolioValue) {
        if (portfolioValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getPositionValue().divide(portfolioValue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Calculate return since inception
     */
    public BigDecimal getReturnSinceInception() {
        if (totalCost.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalPnl.divide(totalCost.abs(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Get exposure (absolute value of position)
     */
    public BigDecimal getExposure() {
        return getPositionValue().abs();
    }
}
