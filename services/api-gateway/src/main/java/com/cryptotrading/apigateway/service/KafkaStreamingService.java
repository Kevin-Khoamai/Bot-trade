package com.cryptotrading.apigateway.service;

import com.cryptotrading.apigateway.controller.WebSocketController;
import com.cryptotrading.apigateway.model.TickerData;
import com.cryptotrading.apigateway.model.TradeData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaStreamingService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaStreamingService.class);

    @Autowired
    private WebSocketController webSocketController;

    @Autowired
    private RealTimeDataService realTimeDataService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Listen to price updates from Kafka and broadcast to WebSocket clients
     */
    @KafkaListener(topics = "price-updates", groupId = "api-gateway-group")
    public void handlePriceUpdate(String message) {
        try {
            logger.debug("Received price update: {}", message);
            
            // Parse the message to TickerData
            TickerData tickerData = objectMapper.readValue(message, TickerData.class);
            
            // Update cache
            realTimeDataService.updateCurrentPrice(tickerData);
            
            // Broadcast to WebSocket clients
            webSocketController.broadcastPriceUpdate(tickerData);
            
            logger.debug("Broadcasted price update for {}: {}", 
                        tickerData.getProductId(), tickerData.getPrice());
            
        } catch (Exception e) {
            logger.error("Error processing price update: {}", message, e);
        }
    }

    /**
     * Listen to trade updates from Kafka and broadcast to WebSocket clients
     */
    @KafkaListener(topics = "coinbase-trades", groupId = "api-gateway-group")
    public void handleTradeUpdate(String message) {
        try {
            logger.debug("Received trade update: {}", message);
            
            // Parse the message to TradeData
            TradeData tradeData = objectMapper.readValue(message, TradeData.class);
            
            // Set exchange if not present
            if (tradeData.getExchange() == null) {
                tradeData.setExchange("COINBASE");
            }
            
            // Update recent trades cache
            realTimeDataService.updateRecentTrade(tradeData);
            
            // Broadcast to WebSocket clients
            webSocketController.broadcastTradeUpdate(tradeData);
            
            logger.debug("Broadcasted trade update for {}: {} @ {}", 
                        tradeData.getProductId(), tradeData.getSize(), tradeData.getPrice());
            
        } catch (Exception e) {
            logger.error("Error processing trade update: {}", message, e);
        }
    }

    /**
     * Listen to Binance trades (if available)
     */
    @KafkaListener(topics = "binance-trades", groupId = "api-gateway-group")
    public void handleBinanceTradeUpdate(String message) {
        try {
            logger.debug("Received Binance trade update: {}", message);
            
            // Parse the message to TradeData
            TradeData tradeData = objectMapper.readValue(message, TradeData.class);
            
            // Set exchange
            tradeData.setExchange("BINANCE");
            
            // Update recent trades cache
            realTimeDataService.updateRecentTrade(tradeData);
            
            // Broadcast to WebSocket clients
            webSocketController.broadcastTradeUpdate(tradeData);
            
        } catch (Exception e) {
            logger.error("Error processing Binance trade update: {}", message, e);
        }
    }

    /**
     * Listen to aggregated market data
     */
    @KafkaListener(topics = "aggregated-market-data", groupId = "api-gateway-group")
    public void handleAggregatedData(String message) {
        try {
            logger.debug("Received aggregated market data: {}", message);
            
            // Process aggregated data and broadcast system updates
            // This could include market summaries, volatility indicators, etc.
            
        } catch (Exception e) {
            logger.error("Error processing aggregated market data: {}", message, e);
        }
    }
}
