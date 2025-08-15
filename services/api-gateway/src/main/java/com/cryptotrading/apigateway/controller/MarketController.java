package com.cryptotrading.apigateway.controller;

import com.cryptotrading.apigateway.service.HistoricalDataService;
import com.cryptotrading.apigateway.service.RealTimeDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class MarketController {

    private static final Logger logger = LoggerFactory.getLogger(MarketController.class);

    @Autowired
    private RealTimeDataService realTimeDataService;

    @Autowired
    private HistoricalDataService historicalDataService;

    /**
     * Get available symbols
     */
    @GetMapping("/symbols")
    public ResponseEntity<List<String>> getAvailableSymbols() {
        try {
            List<String> symbols = historicalDataService.getAvailableSymbols();
            return ResponseEntity.ok(symbols);
        } catch (Exception e) {
            logger.error("Error getting available symbols", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get available exchanges
     */
    @GetMapping("/exchanges")
    public ResponseEntity<List<String>> getAvailableExchanges() {
        try {
            List<String> exchanges = historicalDataService.getAvailableExchanges();
            return ResponseEntity.ok(exchanges);
        } catch (Exception e) {
            logger.error("Error getting available exchanges", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get market overview
     */
    @GetMapping("/market/overview")
    public ResponseEntity<Map<String, Object>> getMarketOverview() {
        try {
            Map<String, Object> overview = historicalDataService.getMarketOverview();
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            logger.error("Error getting market overview", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get market summary (real-time)
     */
    @GetMapping("/market/summary")
    public ResponseEntity<Map<String, Object>> getMarketSummary() {
        try {
            Map<String, Object> summary = realTimeDataService.getMarketSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Error getting market summary", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * System health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        try {
            Map<String, Object> health = Map.of(
                "status", "UP",
                "timestamp", System.currentTimeMillis(),
                "service", "api-gateway"
            );
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            logger.error("Error getting system health", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
