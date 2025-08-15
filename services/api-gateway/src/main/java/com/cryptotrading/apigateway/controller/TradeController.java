package com.cryptotrading.apigateway.controller;

import com.cryptotrading.apigateway.model.TradeData;
import com.cryptotrading.apigateway.service.RealTimeDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
@CrossOrigin(origins = "*")
public class TradeController {

    private static final Logger logger = LoggerFactory.getLogger(TradeController.class);

    @Autowired
    private RealTimeDataService realTimeDataService;

    /**
     * Get recent trades for a specific symbol
     */
    @GetMapping("/recent")
    public ResponseEntity<List<TradeData>> getRecentTrades(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<TradeData> recentTrades = realTimeDataService.getRecentTrades(
                symbol.toUpperCase(), limit);
            return ResponseEntity.ok(recentTrades);
        } catch (Exception e) {
            logger.error("Error getting recent trades for {}", symbol, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get recent trades for all symbols
     */
    @GetMapping("/recent/all")
    public ResponseEntity<List<TradeData>> getAllRecentTrades(
            @RequestParam(defaultValue = "100") int limit) {
        try {
            // This would need to be implemented in RealTimeDataService
            // For now, return empty list
            return ResponseEntity.ok(List.of());
        } catch (Exception e) {
            logger.error("Error getting all recent trades", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
