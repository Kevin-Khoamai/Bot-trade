package com.cryptotrading.dataacquisition.websocket;

import com.cryptotrading.dataacquisition.client.CoinbaseRestClient;
import com.cryptotrading.dataacquisition.service.KafkaProducerService;
import com.cryptotrading.dataacquisition.service.RedisCacheService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket client for Coinbase Pro real-time data streams
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CoinbaseWebSocketClient {

    private final KafkaProducerService kafkaProducerService;
    private final RedisCacheService redisCacheService;
    private final CoinbaseRestClient coinbaseRestClient;
    private final ObjectMapper objectMapper;

    @Value("${exchange.coinbase.websocket-url}")
    private String websocketUrl;

    @Value("${data-collection.symbols:BTC-USD,ETH-USD,ADA-USD}")
    private String symbolsString;

    private List<String> getSymbols() {
        return Arrays.asList(symbolsString.split(","));
    }

    private WebSocketSession session;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private boolean isConnected = false;

    @PostConstruct
    public void initialize() {
        connectToWebSocket();
        
        // Schedule periodic reconnection check
        scheduler.scheduleAtFixedRate(this::checkConnection, 30, 30, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void cleanup() {
        scheduler.shutdown();
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (Exception e) {
                log.error("Error closing WebSocket session: {}", e.getMessage());
            }
        }
    }

    /**
     * Connect to Coinbase WebSocket
     */
    private void connectToWebSocket() {
        try {
            URI uri = URI.create(websocketUrl);
            
            WebSocketConnectionManager connectionManager = new WebSocketConnectionManager(
                new StandardWebSocketClient(), 
                new CoinbaseWebSocketHandler(), 
                uri.toString()
            );
            
            connectionManager.start();
            log.info("Connecting to Coinbase WebSocket: {}", websocketUrl);
            
        } catch (Exception e) {
            log.error("Error connecting to Coinbase WebSocket: {}", e.getMessage());
            scheduleReconnect();
        }
    }

    /**
     * Check connection status and reconnect if needed
     */
    private void checkConnection() {
        if (!isConnected) {
            log.warn("Coinbase WebSocket connection lost, attempting to reconnect...");
            connectToWebSocket();
        }
    }

    /**
     * Schedule reconnection after delay
     */
    private void scheduleReconnect() {
        scheduler.schedule(this::connectToWebSocket, 10, TimeUnit.SECONDS);
    }

    /**
     * WebSocket handler for Coinbase streams
     */
    private class CoinbaseWebSocketHandler implements WebSocketHandler {

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            CoinbaseWebSocketClient.this.session = session;
            isConnected = true;
            log.info("Coinbase WebSocket connection established");
            
            // Subscribe to ticker and match channels
            subscribeToChannels();
        }

        @Override
        public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
            try {
                String payload = message.getPayload().toString();
                processWebSocketMessage(payload);
            } catch (Exception e) {
                log.error("Error processing Coinbase WebSocket message: {}", e.getMessage());
            }
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            log.error("Coinbase WebSocket transport error: {}", exception.getMessage());
            isConnected = false;
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
            log.warn("Coinbase WebSocket connection closed: {}", closeStatus);
            isConnected = false;
            scheduleReconnect();
        }

        @Override
        public boolean supportsPartialMessages() {
            return false;
        }
    }

    /**
     * Subscribe to Coinbase channels
     */
    private void subscribeToChannels() {
        try {
            // Convert Binance symbols to Coinbase product IDs
            List<String> productIds = getSymbols().stream()
                    .map(coinbaseRestClient::convertSymbolToProductId)
                    .toList();
            
            // Subscribe to ticker channel
            String subscribeMessage = String.format("""
                {
                    "type": "subscribe",
                    "product_ids": %s,
                    "channels": ["ticker", "matches"]
                }
                """, objectMapper.writeValueAsString(productIds));
            
            session.sendMessage(new TextMessage(subscribeMessage));
            log.info("Subscribed to Coinbase channels for products: {}", productIds);
            
        } catch (Exception e) {
            log.error("Error subscribing to Coinbase channels: {}", e.getMessage());
        }
    }

    /**
     * Process incoming WebSocket message
     */
    private void processWebSocketMessage(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            String type = jsonNode.get("type").asText();
            
            switch (type) {
                case "ticker" -> processTickerData(jsonNode);
                case "match" -> processMatchData(jsonNode);
                case "subscriptions" -> log.info("Coinbase subscription confirmed: {}", message);
                case "error" -> log.error("Coinbase WebSocket error: {}", message);
                default -> log.debug("Unhandled Coinbase message type: {}", type);
            }
        } catch (Exception e) {
            log.error("Error parsing Coinbase WebSocket message: {}", e.getMessage());
        }
    }

    /**
     * Process ticker data from WebSocket
     */
    private void processTickerData(JsonNode tickerData) {
        try {
            String productId = tickerData.get("product_id").asText();
            BigDecimal price = new BigDecimal(tickerData.get("price").asText());
            BigDecimal bestBid = new BigDecimal(tickerData.get("best_bid").asText());
            BigDecimal bestAsk = new BigDecimal(tickerData.get("best_ask").asText());
            String timeStr = tickerData.get("time").asText();
            
            LocalDateTime timestamp = LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            
            // Create ticker object
            Object tickerObject = new TickerData(productId, price, bestBid, bestAsk, timestamp);
            
            // Cache real-time data
            redisCacheService.cacheRealtimePrice("COINBASE", productId, tickerObject);
            redisCacheService.cacheTickerData("COINBASE", productId, tickerObject);
            
            // Publish to Kafka
            kafkaProducerService.sendPriceUpdate(productId, tickerObject);
            
            log.debug("Processed Coinbase ticker data for {}: {}", productId, price);
            
        } catch (Exception e) {
            log.error("Error processing Coinbase ticker data: {}", e.getMessage());
        }
    }

    /**
     * Process match (trade) data from WebSocket
     */
    private void processMatchData(JsonNode matchData) {
        try {
            String productId = matchData.get("product_id").asText();
            BigDecimal price = new BigDecimal(matchData.get("price").asText());
            BigDecimal size = new BigDecimal(matchData.get("size").asText());
            String side = matchData.get("side").asText();
            String timeStr = matchData.get("time").asText();
            
            LocalDateTime timestamp = LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            
            // Create trade object
            Object tradeObject = new TradeData(productId, price, size, side, timestamp);
            
            // Cache and publish trade data
            redisCacheService.cacheRealtimePrice("COINBASE", productId + "_TRADE", tradeObject);
            kafkaProducerService.sendMessage("coinbase-trades", productId, tradeObject);
            
            log.debug("Processed Coinbase trade data for {}: {} @ {}", productId, size, price);
            
        } catch (Exception e) {
            log.error("Error processing Coinbase match data: {}", e.getMessage());
        }
    }

    /**
     * Get connection status
     */
    public boolean isConnected() {
        return isConnected && session != null && session.isOpen();
    }

    /**
     * Manually trigger reconnection
     */
    public void reconnect() {
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (Exception e) {
                log.error("Error closing session for reconnect: {}", e.getMessage());
            }
        }
        connectToWebSocket();
    }

    /**
     * Ticker data record
     */
    public record TickerData(String productId, BigDecimal price, BigDecimal bestBid, 
                           BigDecimal bestAsk, LocalDateTime timestamp) {}

    /**
     * Trade data record
     */
    public record TradeData(String productId, BigDecimal price, BigDecimal size, 
                          String side, LocalDateTime timestamp) {}
}
