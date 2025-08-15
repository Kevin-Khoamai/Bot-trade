package com.cryptotrading.execution.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a trading order
 */
@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_order_id", unique = true, nullable = false)
    private String clientOrderId;

    @Column(name = "exchange_order_id")
    private String exchangeOrderId;

    @Column(name = "strategy_execution_id")
    private UUID strategyExecutionId;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "exchange", nullable = false)
    private String exchange;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private OrderType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_in_force")
    private TimeInForce timeInForce;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    // Order Quantities
    @Column(name = "quantity", precision = 20, scale = 8, nullable = false)
    private BigDecimal quantity;

    @Column(name = "filled_quantity", precision = 20, scale = 8)
    private BigDecimal filledQuantity = BigDecimal.ZERO;

    @Column(name = "remaining_quantity", precision = 20, scale = 8)
    private BigDecimal remainingQuantity;

    // Order Prices
    @Column(name = "price", precision = 20, scale = 8)
    private BigDecimal price;

    @Column(name = "stop_price", precision = 20, scale = 8)
    private BigDecimal stopPrice;

    @Column(name = "average_fill_price", precision = 20, scale = 8)
    private BigDecimal averageFillPrice;

    // Financial Calculations
    @Column(name = "total_value", precision = 20, scale = 8)
    private BigDecimal totalValue;

    @Column(name = "total_fees", precision = 20, scale = 8)
    private BigDecimal totalFees = BigDecimal.ZERO;

    @Column(name = "net_proceeds", precision = 20, scale = 8)
    private BigDecimal netProceeds;

    // Risk Management
    @Column(name = "max_position_size", precision = 20, scale = 8)
    private BigDecimal maxPositionSize;

    @Column(name = "risk_limit", precision = 20, scale = 8)
    private BigDecimal riskLimit;

    // Execution Context
    @Column(name = "execution_algorithm")
    private String executionAlgorithm;

    @Column(name = "urgency_level")
    private Integer urgencyLevel; // 1-10 scale

    @Column(name = "market_conditions", columnDefinition = "TEXT")
    private String marketConditions; // JSON

    @Column(name = "execution_reason", length = 500)
    private String executionReason;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "first_fill_at")
    private LocalDateTime firstFillAt;

    @Column(name = "last_fill_at")
    private LocalDateTime lastFillAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // Error Handling
    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    // Relationships
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderFill> fills = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderStatusUpdate> statusUpdates = new ArrayList<>();

    /**
     * Order sides
     */
    public enum OrderSide {
        BUY, SELL
    }

    /**
     * Order types
     */
    public enum OrderType {
        MARKET,         // Execute immediately at market price
        LIMIT,          // Execute at specified price or better
        STOP_LOSS,      // Stop loss order
        STOP_LIMIT,     // Stop limit order
        TAKE_PROFIT,    // Take profit order
        ICEBERG,        // Large order split into smaller chunks
        TWAP,           // Time-weighted average price
        VWAP            // Volume-weighted average price
    }

    /**
     * Time in force options
     */
    public enum TimeInForce {
        GTC,    // Good Till Cancelled
        IOC,    // Immediate Or Cancel
        FOK,    // Fill Or Kill
        GTD     // Good Till Date
    }

    /**
     * Order status
     */
    public enum OrderStatus {
        PENDING,            // Order created but not submitted
        SUBMITTED,          // Order submitted to exchange
        ACKNOWLEDGED,       // Order acknowledged by exchange
        PARTIALLY_FILLED,   // Order partially executed
        FILLED,             // Order fully executed
        CANCELLED,          // Order cancelled
        REJECTED,           // Order rejected by exchange
        EXPIRED,            // Order expired
        ERROR               // Error occurred
    }

    /**
     * Check if order is active (can be filled or cancelled)
     */
    public boolean isActive() {
        return status == OrderStatus.SUBMITTED || 
               status == OrderStatus.ACKNOWLEDGED || 
               status == OrderStatus.PARTIALLY_FILLED;
    }

    /**
     * Check if order is completed (no further action possible)
     */
    public boolean isCompleted() {
        return status == OrderStatus.FILLED || 
               status == OrderStatus.CANCELLED || 
               status == OrderStatus.REJECTED || 
               status == OrderStatus.EXPIRED;
    }

    /**
     * Check if order can be cancelled
     */
    public boolean canBeCancelled() {
        return isActive() && status != OrderStatus.PARTIALLY_FILLED;
    }

    /**
     * Calculate fill percentage
     */
    public BigDecimal getFillPercentage() {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal filled = filledQuantity != null ? filledQuantity : BigDecimal.ZERO;
        return filled.divide(quantity, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Update order with fill information
     */
    public void addFill(OrderFill fill) {
        if (fills == null) {
            fills = new ArrayList<>();
        }
        fills.add(fill);
        
        // Update filled quantity
        BigDecimal newFilledQuantity = fills.stream()
                .map(OrderFill::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.filledQuantity = newFilledQuantity;
        this.remainingQuantity = this.quantity.subtract(newFilledQuantity);
        
        // Update average fill price
        BigDecimal totalValue = fills.stream()
                .map(f -> f.getQuantity().multiply(f.getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (newFilledQuantity.compareTo(BigDecimal.ZERO) > 0) {
            this.averageFillPrice = totalValue.divide(newFilledQuantity, 8, java.math.RoundingMode.HALF_UP);
        }
        
        // Update total fees
        this.totalFees = fills.stream()
                .map(OrderFill::getFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Update status
        if (remainingQuantity.compareTo(BigDecimal.ZERO) == 0) {
            this.status = OrderStatus.FILLED;
            this.completedAt = LocalDateTime.now();
        } else {
            this.status = OrderStatus.PARTIALLY_FILLED;
        }
        
        // Update timestamps
        if (this.firstFillAt == null) {
            this.firstFillAt = fill.getTimestamp();
        }
        this.lastFillAt = fill.getTimestamp();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Cancel the order
     */
    public void cancel(String reason) {
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.executionReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark order as rejected
     */
    public void reject(String errorCode, String errorMessage) {
        this.status = OrderStatus.REJECTED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Calculate total order value
     */
    public BigDecimal calculateTotalValue() {
        if (averageFillPrice != null && filledQuantity != null) {
            return averageFillPrice.multiply(filledQuantity);
        } else if (price != null) {
            return price.multiply(quantity);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Calculate net proceeds (value minus fees)
     */
    public BigDecimal calculateNetProceeds() {
        BigDecimal value = calculateTotalValue();
        BigDecimal fees = totalFees != null ? totalFees : BigDecimal.ZERO;
        return side == OrderSide.SELL ? value.subtract(fees) : value.add(fees);
    }
}
