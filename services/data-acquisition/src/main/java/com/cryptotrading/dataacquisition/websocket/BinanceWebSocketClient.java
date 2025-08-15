package com.cryptotrading.dataacquisition.websocket;

import com.cryptotrading.dataacquisition.dto.MarketDataDto;
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
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket client for Binance real-time data streams
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BinanceWebSocketClient {

    private final KafkaProducerService kafkaProducerService;
    private final RedisCacheService redisCacheService;
    private final ObjectMapper objectMapper;

    @Value("${exchange.binance.websocket-url}")
    private String websocketUrl;

    @Value("${data-collection.symbols:BTCUSDT,ETHUSDT,ADAUSDT}")
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
     * Connect to Binance WebSocket
     */
    private void connectToWebSocket() {
        try {
            String streamUrl = buildStreamUrl();
            URI uri = URI.create(streamUrl);
            
            WebSocketConnectionManager connectionManager = new WebSocketConnectionManager(
                new StandardWebSocketClient(), 
                new BinanceWebSocketHandler(), 
                uri.toString()
            );
            
            connectionManager.start();
            log.info("Connecting to Binance WebSocket: {}", streamUrl);
            
        } catch (Exception e) {
            log.error("Error connecting to Binance WebSocket: {}", e.getMessage());
            scheduleReconnect();
        }
    }

    /**
     * Build WebSocket stream URL for multiple symbols
     */
    private String buildStreamUrl() {
        StringBuilder streamBuilder = new StringBuilder(websocketUrl);
        List<String> symbols = getSymbols();

        for (int i = 0; i < symbols.size(); i++) {
            String symbol = symbols.get(i).toLowerCase();
            streamBuilder.append(symbol).append("@kline_1m");
            if (i < symbols.size() - 1) {
                streamBuilder.append("/");
            }
        }

        return streamBuilder.toString();
    }

    /**
     * Check connection status and reconnect if needed
     */
    private void checkConnection() {
        if (!isConnected) {
            log.warn("WebSocket connection lost, attempting to reconnect...");
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
     * WebSocket handler for Binance streams
     */
    private class BinanceWebSocketHandler implements WebSocketHandler {

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            BinanceWebSocketClient.this.session = session;
            isConnected = true;
            log.info("Binance WebSocket connection established");
        }

        @Override
        public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
            try {
                String payload = message.getPayload().toString();
                processWebSocketMessage(payload);
            } catch (Exception e) {
                log.error("Error processing WebSocket message: {}", e.getMessage());
            }
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            log.error("Binance WebSocket transport error: {}", exception.getMessage());
            isConnected = false;
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
            log.warn("Binance WebSocket connection closed: {}", closeStatus);
            isConnected = false;
            scheduleReconnect();
        }

        @Override
        public boolean supportsPartialMessages() {
            return false;
        }
    }

    /**
     * Process incoming WebSocket message
     */
    private void processWebSocketMessage(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            
            if (jsonNode.has("stream") && jsonNode.has("data")) {
                String stream = jsonNode.get("stream").asText();
                JsonNode data = jsonNode.get("data");
                
                if (stream.contains("@kline")) {
                    processKlineData(data);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing WebSocket message: {}", e.getMessage());
        }
    }

    /**
     * Process kline (candlestick) data from WebSocket
     */
    private void processKlineData(JsonNode klineData) {
        try {
            JsonNode k = klineData.get("k");
            
            String symbol = k.get("s").asText();
            long openTime = k.get("t").asLong();
            BigDecimal open = new BigDecimal(k.get("o").asText());
            BigDecimal high = new BigDecimal(k.get("h").asText());
            BigDecimal low = new BigDecimal(k.get("l").asText());
            BigDecimal close = new BigDecimal(k.get("c").asText());
            BigDecimal volume = new BigDecimal(k.get("v").asText());
            boolean isClosed = k.get("x").asBoolean(); // Is this kline closed?
            
            // Only process closed klines for historical data
            if (isClosed) {
                MarketDataDto marketData = MarketDataDto.builder()
                        .exchange("BINANCE")
                        .symbol(symbol)
                        .timestamp(LocalDateTime.ofEpochSecond(openTime / 1000, 0, java.time.ZoneOffset.UTC))
                        .openPrice(open)
                        .highPrice(high)
                        .lowPrice(low)
                        .closePrice(close)
                        .volume(volume)
                        .dataType("KLINE")
                        .source("WEBSOCKET")
                        .build();
                
                // Cache real-time data
                redisCacheService.cacheRealtimePrice("BINANCE", symbol, marketData);
                
                // Publish to Kafka
                kafkaProducerService.sendMarketData(marketData);
                
                log.debug("Processed WebSocket kline data for {}: {}", symbol, close);
            }
            
        } catch (Exception e) {
            log.error("Error processing kline data: {}", e.getMessage());
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
}
