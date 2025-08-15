package com.cryptotrading.execution.service;

import com.binance.connector.client.impl.SpotClientImpl;
import com.cryptotrading.execution.model.Order;
import com.cryptotrading.execution.model.OrderFill;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service for integrating with cryptocurrency exchanges
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeGatewayService {

    @Value("${exchange.binance.api-key}")
    private String binanceApiKey;

    @Value("${exchange.binance.secret-key}")
    private String binanceSecretKey;

    @Value("${exchange.binance.testnet:true}")
    private boolean binanceTestnet;

    private final ObjectMapper objectMapper;
    private final OrderExecutionService orderExecutionService;

    // Rate limiting buckets for different exchanges
    private final Bucket binanceBucket = createBinanceRateLimitBucket();

    /**
     * Submit order to appropriate exchange
     */
    public String submitOrder(Order order) {
        switch (order.getExchange().toUpperCase()) {
            case "BINANCE":
                return submitBinanceOrder(order);
            case "COINBASE":
                return submitCoinbaseOrder(order);
            default:
                throw new IllegalArgumentException("Unsupported exchange: " + order.getExchange());
        }
    }

    /**
     * Cancel order on appropriate exchange
     */
    public void cancelOrder(Order order) {
        switch (order.getExchange().toUpperCase()) {
            case "BINANCE":
                cancelBinanceOrder(order);
                break;
            case "COINBASE":
                cancelCoinbaseOrder(order);
                break;
            default:
                throw new IllegalArgumentException("Unsupported exchange: " + order.getExchange());
        }
    }

    /**
     * Submit order to Binance
     */
    private String submitBinanceOrder(Order order) {
        try {
            // Rate limiting
            if (!binanceBucket.tryConsume(1)) {
                throw new RuntimeException("Rate limit exceeded for Binance");
            }

            SpotClientImpl client = new SpotClientImpl(binanceApiKey, binanceSecretKey, binanceTestnet);

            // Prepare order parameters
            LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("symbol", convertSymbolToBinanceFormat(order.getSymbol()));
            parameters.put("side", order.getSide().name());
            parameters.put("type", convertOrderTypeToBinanceFormat(order.getType()));
            parameters.put("quantity", order.getQuantity().toPlainString());
            parameters.put("newClientOrderId", order.getClientOrderId());

            // Add price for limit orders
            if (order.getType() == Order.OrderType.LIMIT && order.getPrice() != null) {
                parameters.put("price", order.getPrice().toPlainString());
                parameters.put("timeInForce", convertTimeInForceToBinanceFormat(order.getTimeInForce()));
            }

            // Add stop price for stop orders
            if (order.getStopPrice() != null) {
                parameters.put("stopPrice", order.getStopPrice().toPlainString());
            }

            // Submit order
            String response = client.createOrder(parameters);
            log.info("Binance order response: {}", response);

            // Parse response to get order ID
            JsonNode responseNode = objectMapper.readTree(response);
            String exchangeOrderId = responseNode.get("orderId").asText();

            log.info("Successfully submitted order to Binance: {} -> {}", 
                    order.getClientOrderId(), exchangeOrderId);

            return exchangeOrderId;

        } catch (Exception e) {
            log.error("Error submitting order to Binance: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to submit order to Binance: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel order on Binance
     */
    private void cancelBinanceOrder(Order order) {
        try {
            // Rate limiting
            if (!binanceBucket.tryConsume(1)) {
                throw new RuntimeException("Rate limit exceeded for Binance");
            }

            SpotClientImpl client = new SpotClientImpl(binanceApiKey, binanceSecretKey, binanceTestnet);

            LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("symbol", convertSymbolToBinanceFormat(order.getSymbol()));
            parameters.put("orderId", order.getExchangeOrderId());

            String response = client.cancelOrder(parameters);
            log.info("Binance cancel response: {}", response);

            log.info("Successfully cancelled order on Binance: {}", order.getClientOrderId());

        } catch (Exception e) {
            log.error("Error cancelling order on Binance: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to cancel order on Binance: " + e.getMessage(), e);
        }
    }

    /**
     * Submit order to Coinbase (placeholder implementation)
     */
    private String submitCoinbaseOrder(Order order) {
        // TODO: Implement Coinbase Pro API integration
        log.warn("Coinbase order submission not yet implemented");
        throw new UnsupportedOperationException("Coinbase integration not yet implemented");
    }

    /**
     * Cancel order on Coinbase (placeholder implementation)
     */
    private void cancelCoinbaseOrder(Order order) {
        // TODO: Implement Coinbase Pro API integration
        log.warn("Coinbase order cancellation not yet implemented");
        throw new UnsupportedOperationException("Coinbase integration not yet implemented");
    }

    /**
     * Handle order status update from exchange WebSocket
     */
    public void handleExchangeOrderUpdate(String exchange, String orderUpdateJson) {
        try {
            JsonNode updateNode = objectMapper.readTree(orderUpdateJson);
            
            switch (exchange.toUpperCase()) {
                case "BINANCE":
                    handleBinanceOrderUpdate(updateNode);
                    break;
                case "COINBASE":
                    handleCoinbaseOrderUpdate(updateNode);
                    break;
                default:
                    log.warn("Unknown exchange for order update: {}", exchange);
            }

        } catch (Exception e) {
            log.error("Error handling exchange order update: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle Binance order update
     */
    private void handleBinanceOrderUpdate(JsonNode updateNode) {
        try {
            String eventType = updateNode.get("e").asText();
            
            if ("executionReport".equals(eventType)) {
                String exchangeOrderId = updateNode.get("i").asText();
                String orderStatus = updateNode.get("X").asText();
                
                // Handle different order statuses
                switch (orderStatus) {
                    case "NEW":
                        orderExecutionService.handleOrderAcknowledgment(exchangeOrderId, "Order acknowledged by Binance");
                        break;
                    case "PARTIALLY_FILLED":
                    case "FILLED":
                        handleBinanceOrderFill(updateNode);
                        break;
                    case "CANCELED":
                        // Handle cancellation
                        break;
                    case "REJECTED":
                        // Handle rejection
                        break;
                }
            }

        } catch (Exception e) {
            log.error("Error handling Binance order update: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle Binance order fill
     */
    private void handleBinanceOrderFill(JsonNode updateNode) {
        try {
            String exchangeOrderId = updateNode.get("i").asText();
            String tradeId = updateNode.get("t").asText();
            
            BigDecimal fillQuantity = new BigDecimal(updateNode.get("l").asText());
            BigDecimal fillPrice = new BigDecimal(updateNode.get("L").asText());
            BigDecimal commission = new BigDecimal(updateNode.get("n").asText());
            String commissionAsset = updateNode.get("N").asText();
            
            long timestamp = updateNode.get("T").asLong();
            LocalDateTime fillTime = LocalDateTime.ofEpochSecond(timestamp / 1000, 0, ZoneOffset.UTC);

            OrderFill fill = OrderFill.builder()
                    .exchangeFillId(tradeId)
                    .exchangeTradeId(tradeId)
                    .symbol(updateNode.get("s").asText())
                    .exchange("BINANCE")
                    .side(Order.OrderSide.valueOf(updateNode.get("S").asText()))
                    .quantity(fillQuantity)
                    .price(fillPrice)
                    .fee(commission)
                    .feeCurrency(commissionAsset)
                    .timestamp(fillTime)
                    .liquidityType(updateNode.get("m").asBoolean() ? OrderFill.LiquidityType.MAKER : OrderFill.LiquidityType.TAKER)
                    .build();

            orderExecutionService.handleOrderFill(exchangeOrderId, fill);

        } catch (Exception e) {
            log.error("Error handling Binance order fill: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle Coinbase order update (placeholder)
     */
    private void handleCoinbaseOrderUpdate(JsonNode updateNode) {
        // TODO: Implement Coinbase order update handling
        log.warn("Coinbase order update handling not yet implemented");
    }

    /**
     * Convert symbol to Binance format
     */
    private String convertSymbolToBinanceFormat(String symbol) {
        // Convert BTCUSDT format to Binance format (already correct)
        return symbol.replace("-", "").toUpperCase();
    }

    /**
     * Convert order type to Binance format
     */
    private String convertOrderTypeToBinanceFormat(Order.OrderType orderType) {
        return switch (orderType) {
            case MARKET -> "MARKET";
            case LIMIT -> "LIMIT";
            case STOP_LOSS -> "STOP_LOSS";
            case STOP_LIMIT -> "STOP_LOSS_LIMIT";
            case TAKE_PROFIT -> "TAKE_PROFIT";
            default -> "MARKET";
        };
    }

    /**
     * Convert time in force to Binance format
     */
    private String convertTimeInForceToBinanceFormat(Order.TimeInForce timeInForce) {
        return switch (timeInForce) {
            case GTC -> "GTC";
            case IOC -> "IOC";
            case FOK -> "FOK";
            default -> "GTC";
        };
    }

    /**
     * Create rate limit bucket for Binance (1200 requests per minute)
     */
    private Bucket createBinanceRateLimitBucket() {
        Bandwidth limit = Bandwidth.classic(1200, Refill.intervally(1200, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Get order status from exchange
     */
    public Map<String, Object> getOrderStatus(Order order) {
        switch (order.getExchange().toUpperCase()) {
            case "BINANCE":
                return getBinanceOrderStatus(order);
            case "COINBASE":
                return getCoinbaseOrderStatus(order);
            default:
                throw new IllegalArgumentException("Unsupported exchange: " + order.getExchange());
        }
    }

    /**
     * Get Binance order status
     */
    private Map<String, Object> getBinanceOrderStatus(Order order) {
        try {
            SpotClientImpl client = new SpotClientImpl(binanceApiKey, binanceSecretKey, binanceTestnet);

            LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("symbol", convertSymbolToBinanceFormat(order.getSymbol()));
            parameters.put("orderId", order.getExchangeOrderId());

            String response = client.getOrder(parameters);
            JsonNode responseNode = objectMapper.readTree(response);

            return objectMapper.convertValue(responseNode, Map.class);

        } catch (Exception e) {
            log.error("Error getting Binance order status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get order status from Binance: " + e.getMessage(), e);
        }
    }

    /**
     * Get Coinbase order status (placeholder)
     */
    private Map<String, Object> getCoinbaseOrderStatus(Order order) {
        // TODO: Implement Coinbase order status retrieval
        throw new UnsupportedOperationException("Coinbase integration not yet implemented");
    }
}
