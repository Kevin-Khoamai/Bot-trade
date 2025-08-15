package com.cryptotrading.apigateway.controller;

import com.cryptotrading.apigateway.model.TickerData;
import com.cryptotrading.apigateway.model.TradeData;
import com.cryptotrading.apigateway.service.RealTimeDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class WebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private RealTimeDataService realTimeDataService;

    /**
     * Handle client subscription to price updates
     */
    @MessageMapping("/subscribe.prices")
    @SendTo("/topic/prices")
    public String subscribeToPrices(@Payload Map<String, Object> subscription, 
                                   SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        logger.info("Client {} subscribed to price updates", sessionId);
        
        // Get current prices for immediate response
        return "Subscribed to price updates";
    }

    /**
     * Handle client subscription to trade updates
     */
    @MessageMapping("/subscribe.trades")
    @SendTo("/topic/trades")
    public String subscribeToTrades(@Payload Map<String, Object> subscription,
                                   SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        logger.info("Client {} subscribed to trade updates", sessionId);
        
        return "Subscribed to trade updates";
    }

    /**
     * Handle client subscription to specific symbol
     */
    @MessageMapping("/subscribe.symbol")
    public void subscribeToSymbol(@Payload Map<String, Object> subscription,
                                 SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String symbol = (String) subscription.get("symbol");
        
        logger.info("Client {} subscribed to symbol: {}", sessionId, symbol);
        
        // Send current data for the symbol
        TickerData currentTicker = realTimeDataService.getCurrentPrice(symbol);
        if (currentTicker != null) {
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/symbol/" + symbol, currentTicker);
        }
    }

    /**
     * Handle client unsubscription
     */
    @MessageMapping("/unsubscribe")
    public void unsubscribe(@Payload Map<String, Object> unsubscription,
                           SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String type = (String) unsubscription.get("type");
        
        logger.info("Client {} unsubscribed from: {}", sessionId, type);
    }

    /**
     * Broadcast price update to all subscribers
     */
    public void broadcastPriceUpdate(TickerData tickerData) {
        messagingTemplate.convertAndSend("/topic/prices", tickerData);
        messagingTemplate.convertAndSend("/topic/symbol/" + tickerData.getProductId(), tickerData);
    }

    /**
     * Broadcast trade update to all subscribers
     */
    public void broadcastTradeUpdate(TradeData tradeData) {
        messagingTemplate.convertAndSend("/topic/trades", tradeData);
        messagingTemplate.convertAndSend("/topic/symbol/" + tradeData.getProductId() + "/trades", tradeData);
    }

    /**
     * Send system status update
     */
    public void broadcastSystemStatus(Map<String, Object> status) {
        messagingTemplate.convertAndSend("/topic/system", status);
    }
}
