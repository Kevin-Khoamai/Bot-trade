package com.cryptotrading.execution.service;

import com.cryptotrading.execution.model.Order;
import com.cryptotrading.execution.model.OrderFill;
import com.cryptotrading.execution.model.OrderStatusUpdate;
import com.cryptotrading.execution.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Core service for order execution and lifecycle management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderExecutionService {

    private final OrderRepository orderRepository;
    private final ExchangeGatewayService exchangeGatewayService;
    private final RiskControlService riskControlService;
    private final PositionService positionService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderValidationService orderValidationService;

    /**
     * Listen to execution orders from Strategy Service
     */
    @KafkaListener(topics = "execution-orders", groupId = "execution-service-group")
    public void processExecutionOrder(Map<String, Object> executionData) {
        try {
            log.info("Received execution order: {}", executionData);

            // Convert execution data to Order
            Order order = convertExecutionDataToOrder(executionData);
            
            // Execute the order asynchronously
            executeOrderAsync(order);

        } catch (Exception e) {
            log.error("Error processing execution order: {}", e.getMessage(), e);
        }
    }

    /**
     * Execute order asynchronously
     */
    @Transactional
    public CompletableFuture<Order> executeOrderAsync(Order order) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeOrder(order);
            } catch (Exception e) {
                log.error("Error executing order {}: {}", order.getClientOrderId(), e.getMessage(), e);
                order.reject("EXECUTION_ERROR", e.getMessage());
                orderRepository.save(order);
                return order;
            }
        });
    }

    /**
     * Execute order synchronously
     */
    @Transactional
    public Order executeOrder(Order order) {
        try {
            log.info("Executing order: {} {} {} at {}", 
                    order.getSide(), order.getQuantity(), order.getSymbol(), order.getPrice());

            // Step 1: Validate order
            if (!orderValidationService.validateOrder(order)) {
                order.reject("VALIDATION_FAILED", "Order validation failed");
                orderRepository.save(order);
                return order;
            }

            // Step 2: Pre-trade risk checks
            if (!riskControlService.preTradeRiskCheck(order)) {
                order.reject("RISK_CHECK_FAILED", "Pre-trade risk check failed");
                orderRepository.save(order);
                return order;
            }

            // Step 3: Save order as pending
            order.setStatus(Order.OrderStatus.PENDING);
            order = orderRepository.save(order);

            // Step 4: Submit to exchange
            Order submittedOrder = submitOrderToExchange(order);

            // Step 5: Update position tracking
            positionService.updatePositionForOrder(submittedOrder);

            // Step 6: Publish order status update
            publishOrderStatusUpdate(submittedOrder);

            return submittedOrder;

        } catch (Exception e) {
            log.error("Error executing order {}: {}", order.getClientOrderId(), e.getMessage(), e);
            order.reject("EXECUTION_ERROR", e.getMessage());
            orderRepository.save(order);
            throw e;
        }
    }

    /**
     * Submit order to exchange
     */
    private Order submitOrderToExchange(Order order) {
        try {
            // Submit to appropriate exchange
            String exchangeOrderId = exchangeGatewayService.submitOrder(order);
            
            // Update order with exchange ID and status
            order.setExchangeOrderId(exchangeOrderId);
            order.setStatus(Order.OrderStatus.SUBMITTED);
            order.setSubmittedAt(LocalDateTime.now());
            
            // Add status update
            OrderStatusUpdate statusUpdate = OrderStatusUpdate.createSubmissionUpdate(order, "SYSTEM");
            order.getStatusUpdates().add(statusUpdate);
            
            order = orderRepository.save(order);
            
            log.info("Order {} submitted to {} with exchange ID: {}", 
                    order.getClientOrderId(), order.getExchange(), exchangeOrderId);
            
            return order;

        } catch (Exception e) {
            log.error("Error submitting order {} to exchange: {}", order.getClientOrderId(), e.getMessage(), e);
            order.reject("SUBMISSION_FAILED", e.getMessage());
            orderRepository.save(order);
            throw e;
        }
    }

    /**
     * Handle order acknowledgment from exchange
     */
    @Transactional
    public void handleOrderAcknowledgment(String exchangeOrderId, String exchangeMessage) {
        try {
            Order order = orderRepository.findByExchangeOrderId(exchangeOrderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + exchangeOrderId));

            order.setStatus(Order.OrderStatus.ACKNOWLEDGED);
            order.setAcknowledgedAt(LocalDateTime.now());

            // Add status update
            OrderStatusUpdate statusUpdate = OrderStatusUpdate.createAcknowledgmentUpdate(order, exchangeMessage);
            order.getStatusUpdates().add(statusUpdate);

            orderRepository.save(order);

            log.info("Order {} acknowledged by exchange", order.getClientOrderId());

            // Publish status update
            publishOrderStatusUpdate(order);

        } catch (Exception e) {
            log.error("Error handling order acknowledgment for {}: {}", exchangeOrderId, e.getMessage(), e);
        }
    }

    /**
     * Handle order fill from exchange
     */
    @Transactional
    public void handleOrderFill(String exchangeOrderId, OrderFill fill) {
        try {
            Order order = orderRepository.findByExchangeOrderId(exchangeOrderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + exchangeOrderId));

            // Add fill to order
            fill.setOrder(order);
            order.addFill(fill);

            // Post-trade risk check
            riskControlService.postTradeRiskCheck(order, fill);

            // Update position
            positionService.updatePositionForFill(order, fill);

            orderRepository.save(order);

            log.info("Order {} filled: {} at {} (total filled: {})", 
                    order.getClientOrderId(), fill.getQuantity(), fill.getPrice(), order.getFilledQuantity());

            // Publish fill update
            publishOrderFillUpdate(order, fill);

            // If order is fully filled, handle completion
            if (order.getStatus() == Order.OrderStatus.FILLED) {
                handleOrderCompletion(order);
            }

        } catch (Exception e) {
            log.error("Error handling order fill for {}: {}", exchangeOrderId, e.getMessage(), e);
        }
    }

    /**
     * Handle order completion
     */
    private void handleOrderCompletion(Order order) {
        try {
            // Calculate final metrics
            order.setTotalValue(order.calculateTotalValue());
            order.setNetProceeds(order.calculateNetProceeds());

            // Final risk check
            riskControlService.finalRiskCheck(order);

            // Update position
            positionService.finalizePositionForOrder(order);

            orderRepository.save(order);

            log.info("Order {} completed: {} {} at avg price {} (total value: {})", 
                    order.getClientOrderId(), order.getFilledQuantity(), order.getSymbol(), 
                    order.getAverageFillPrice(), order.getTotalValue());

            // Publish completion event
            publishOrderCompletionUpdate(order);

        } catch (Exception e) {
            log.error("Error handling order completion for {}: {}", order.getClientOrderId(), e.getMessage(), e);
        }
    }

    /**
     * Cancel order
     */
    @Transactional
    public void cancelOrder(String clientOrderId, String reason) {
        try {
            Order order = orderRepository.findByClientOrderId(clientOrderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + clientOrderId));

            if (!order.canBeCancelled()) {
                throw new IllegalStateException("Order cannot be cancelled: " + order.getStatus());
            }

            // Cancel on exchange
            exchangeGatewayService.cancelOrder(order);

            // Update order status
            order.cancel(reason);

            // Add status update
            OrderStatusUpdate statusUpdate = OrderStatusUpdate.createCancellationUpdate(order, reason, "USER");
            order.getStatusUpdates().add(statusUpdate);

            orderRepository.save(order);

            log.info("Order {} cancelled: {}", clientOrderId, reason);

            // Publish cancellation event
            publishOrderStatusUpdate(order);

        } catch (Exception e) {
            log.error("Error cancelling order {}: {}", clientOrderId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Convert execution data from Strategy Service to Order
     */
    private Order convertExecutionDataToOrder(Map<String, Object> executionData) {
        return Order.builder()
                .clientOrderId(UUID.randomUUID().toString())
                .strategyExecutionId(UUID.fromString((String) executionData.get("strategyExecutionId")))
                .symbol((String) executionData.get("symbol"))
                .exchange((String) executionData.get("exchange"))
                .side(Order.OrderSide.valueOf((String) executionData.get("side")))
                .type(Order.OrderType.valueOf((String) executionData.getOrDefault("type", "MARKET")))
                .timeInForce(Order.TimeInForce.valueOf((String) executionData.getOrDefault("timeInForce", "GTC")))
                .quantity(new BigDecimal(executionData.get("quantity").toString()))
                .price(executionData.get("price") != null ? new BigDecimal(executionData.get("price").toString()) : null)
                .stopPrice(executionData.get("stopPrice") != null ? new BigDecimal(executionData.get("stopPrice").toString()) : null)
                .executionReason((String) executionData.get("reason"))
                .urgencyLevel((Integer) executionData.getOrDefault("urgencyLevel", 5))
                .status(Order.OrderStatus.PENDING)
                .build();
    }

    /**
     * Publish order status update to Kafka
     */
    private void publishOrderStatusUpdate(Order order) {
        try {
            Map<String, Object> updateData = Map.of(
                    "orderId", order.getId().toString(),
                    "clientOrderId", order.getClientOrderId(),
                    "status", order.getStatus().name(),
                    "symbol", order.getSymbol(),
                    "side", order.getSide().name(),
                    "quantity", order.getQuantity(),
                    "filledQuantity", order.getFilledQuantity(),
                    "timestamp", LocalDateTime.now()
            );

            kafkaTemplate.send("order-status-updates", order.getSymbol(), updateData);
            log.debug("Published order status update for: {}", order.getClientOrderId());

        } catch (Exception e) {
            log.error("Error publishing order status update: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish order fill update to Kafka
     */
    private void publishOrderFillUpdate(Order order, OrderFill fill) {
        try {
            Map<String, Object> fillData = Map.of(
                    "orderId", order.getId().toString(),
                    "clientOrderId", order.getClientOrderId(),
                    "fillId", fill.getId().toString(),
                    "symbol", order.getSymbol(),
                    "side", order.getSide().name(),
                    "quantity", fill.getQuantity(),
                    "price", fill.getPrice(),
                    "fee", fill.getFee(),
                    "timestamp", fill.getTimestamp()
            );

            kafkaTemplate.send("order-fills", order.getSymbol(), fillData);
            log.debug("Published order fill update for: {}", order.getClientOrderId());

        } catch (Exception e) {
            log.error("Error publishing order fill update: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish order completion update to Kafka
     */
    private void publishOrderCompletionUpdate(Order order) {
        try {
            Map<String, Object> completionData = Map.of(
                    "orderId", order.getId().toString(),
                    "clientOrderId", order.getClientOrderId(),
                    "symbol", order.getSymbol(),
                    "side", order.getSide().name(),
                    "totalQuantity", order.getQuantity(),
                    "filledQuantity", order.getFilledQuantity(),
                    "averagePrice", order.getAverageFillPrice(),
                    "totalValue", order.getTotalValue(),
                    "totalFees", order.getTotalFees(),
                    "netProceeds", order.getNetProceeds(),
                    "completedAt", order.getCompletedAt()
            );

            kafkaTemplate.send("order-completions", order.getSymbol(), completionData);
            log.debug("Published order completion update for: {}", order.getClientOrderId());

        } catch (Exception e) {
            log.error("Error publishing order completion update: {}", e.getMessage(), e);
        }
    }
}
