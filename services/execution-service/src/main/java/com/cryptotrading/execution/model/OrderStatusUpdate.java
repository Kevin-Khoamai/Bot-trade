package com.cryptotrading.execution.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing order status changes for audit trail
 */
@Entity
@Table(name = "order_status_updates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    private Order.OrderStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private Order.OrderStatus newStatus;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "source")
    private String source; // EXCHANGE, SYSTEM, USER, RISK_MANAGEMENT

    @Column(name = "exchange_message", length = 1000)
    private String exchangeMessage;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "additional_data", columnDefinition = "TEXT")
    private String additionalData; // JSON for extra context

    @CreationTimestamp
    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    /**
     * Create status update for order submission
     */
    public static OrderStatusUpdate createSubmissionUpdate(Order order, String source) {
        return OrderStatusUpdate.builder()
                .order(order)
                .previousStatus(Order.OrderStatus.PENDING)
                .newStatus(Order.OrderStatus.SUBMITTED)
                .reason("Order submitted to exchange")
                .source(source)
                .build();
    }

    /**
     * Create status update for order acknowledgment
     */
    public static OrderStatusUpdate createAcknowledgmentUpdate(Order order, String exchangeMessage) {
        return OrderStatusUpdate.builder()
                .order(order)
                .previousStatus(Order.OrderStatus.SUBMITTED)
                .newStatus(Order.OrderStatus.ACKNOWLEDGED)
                .reason("Order acknowledged by exchange")
                .source("EXCHANGE")
                .exchangeMessage(exchangeMessage)
                .build();
    }

    /**
     * Create status update for order fill
     */
    public static OrderStatusUpdate createFillUpdate(Order order, Order.OrderStatus newStatus, String fillDetails) {
        return OrderStatusUpdate.builder()
                .order(order)
                .previousStatus(order.getStatus())
                .newStatus(newStatus)
                .reason("Order filled")
                .source("EXCHANGE")
                .exchangeMessage(fillDetails)
                .build();
    }

    /**
     * Create status update for order cancellation
     */
    public static OrderStatusUpdate createCancellationUpdate(Order order, String reason, String source) {
        return OrderStatusUpdate.builder()
                .order(order)
                .previousStatus(order.getStatus())
                .newStatus(Order.OrderStatus.CANCELLED)
                .reason(reason)
                .source(source)
                .build();
    }

    /**
     * Create status update for order rejection
     */
    public static OrderStatusUpdate createRejectionUpdate(Order order, String errorCode, String errorMessage) {
        return OrderStatusUpdate.builder()
                .order(order)
                .previousStatus(order.getStatus())
                .newStatus(Order.OrderStatus.REJECTED)
                .reason("Order rejected by exchange")
                .source("EXCHANGE")
                .errorCode(errorCode)
                .exchangeMessage(errorMessage)
                .build();
    }

    /**
     * Create status update for system error
     */
    public static OrderStatusUpdate createErrorUpdate(Order order, String errorCode, String errorMessage) {
        return OrderStatusUpdate.builder()
                .order(order)
                .previousStatus(order.getStatus())
                .newStatus(Order.OrderStatus.ERROR)
                .reason("System error occurred")
                .source("SYSTEM")
                .errorCode(errorCode)
                .exchangeMessage(errorMessage)
                .build();
    }

    /**
     * Check if this is a terminal status update
     */
    public boolean isTerminalUpdate() {
        return newStatus == Order.OrderStatus.FILLED ||
               newStatus == Order.OrderStatus.CANCELLED ||
               newStatus == Order.OrderStatus.REJECTED ||
               newStatus == Order.OrderStatus.EXPIRED ||
               newStatus == Order.OrderStatus.ERROR;
    }

    /**
     * Check if this is an error-related update
     */
    public boolean isErrorUpdate() {
        return newStatus == Order.OrderStatus.REJECTED ||
               newStatus == Order.OrderStatus.ERROR ||
               errorCode != null;
    }

    /**
     * Check if this update indicates successful progression
     */
    public boolean isSuccessfulProgression() {
        if (previousStatus == null) {
            return true;
        }

        // Define successful progressions
        return (previousStatus == Order.OrderStatus.PENDING && newStatus == Order.OrderStatus.SUBMITTED) ||
               (previousStatus == Order.OrderStatus.SUBMITTED && newStatus == Order.OrderStatus.ACKNOWLEDGED) ||
               (previousStatus == Order.OrderStatus.ACKNOWLEDGED && newStatus == Order.OrderStatus.PARTIALLY_FILLED) ||
               (previousStatus == Order.OrderStatus.PARTIALLY_FILLED && newStatus == Order.OrderStatus.FILLED) ||
               (previousStatus == Order.OrderStatus.ACKNOWLEDGED && newStatus == Order.OrderStatus.FILLED);
    }

    /**
     * Get duration since previous status (if available)
     */
    public Long getDurationFromPreviousStatus() {
        // This would require additional logic to track timing between status changes
        // For now, return null - could be enhanced with more sophisticated tracking
        return null;
    }

    /**
     * Get human-readable status transition description
     */
    public String getStatusTransitionDescription() {
        String prev = previousStatus != null ? previousStatus.name() : "NONE";
        return String.format("%s → %s", prev, newStatus.name());
    }

    /**
     * Check if this update requires immediate attention
     */
    public boolean requiresAttention() {
        return isErrorUpdate() || 
               newStatus == Order.OrderStatus.REJECTED ||
               (errorCode != null && !errorCode.isEmpty());
    }

    /**
     * Get priority level for this update (1-5, 5 being highest)
     */
    public int getPriorityLevel() {
        if (newStatus == Order.OrderStatus.ERROR) {
            return 5; // Critical
        } else if (newStatus == Order.OrderStatus.REJECTED) {
            return 4; // High
        } else if (newStatus == Order.OrderStatus.FILLED) {
            return 3; // Medium-High
        } else if (newStatus == Order.OrderStatus.PARTIALLY_FILLED) {
            return 2; // Medium
        } else {
            return 1; // Low
        }
    }
}
