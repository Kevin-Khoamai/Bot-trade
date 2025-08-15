package com.cryptotrading.apigateway.controller;

import com.cryptotrading.apigateway.model.PriceData;
import com.cryptotrading.apigateway.model.TickerData;
import com.cryptotrading.apigateway.service.HistoricalDataService;
import com.cryptotrading.apigateway.service.RealTimeDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prices")
@CrossOrigin(origins = "*")
public class PriceController {

    private static final Logger logger = LoggerFactory.getLogger(PriceController.class);

    @Autowired
    private RealTimeDataService realTimeDataService;

    @Autowired
    private HistoricalDataService historicalDataService;

    /**
     * Get current prices for all symbols
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, TickerData>> getCurrentPrices() {
        try {
            Map<String, TickerData> currentPrices = realTimeDataService.getAllCurrentPrices();
            return ResponseEntity.ok(currentPrices);
        } catch (Exception e) {
            logger.error("Error getting current prices", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get current price for specific symbol
     */
    @GetMapping("/current/{symbol}")
    public ResponseEntity<TickerData> getCurrentPrice(@PathVariable String symbol) {
        try {
            TickerData currentPrice = realTimeDataService.getCurrentPrice(symbol.toUpperCase());
            if (currentPrice != null) {
                return ResponseEntity.ok(currentPrice);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error getting current price for {}", symbol, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get historical prices
     */
    @GetMapping("/history")
    public ResponseEntity<List<PriceData>> getHistoricalPrices(
            @RequestParam String symbol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            List<PriceData> historicalPrices = historicalDataService.getHistoricalPrices(
                symbol.toUpperCase(), startTime, endTime);
            return ResponseEntity.ok(historicalPrices);
        } catch (Exception e) {
            logger.error("Error getting historical prices for {}", symbol, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get historical prices with pagination
     */
    @GetMapping("/history/paginated")
    public ResponseEntity<Page<PriceData>> getHistoricalPricesPaginated(
            @RequestParam String symbol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        try {
            Page<PriceData> historicalPrices = historicalDataService.getHistoricalPrices(
                symbol.toUpperCase(), startTime, endTime, page, size);
            return ResponseEntity.ok(historicalPrices);
        } catch (Exception e) {
            logger.error("Error getting paginated historical prices for {}", symbol, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get OHLCV data for candlestick charts
     */
    @GetMapping("/ohlcv")
    public ResponseEntity<List<PriceData>> getOHLCVData(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "1h") String interval,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = calculateStartTime(endTime, interval, limit);
            
            List<PriceData> ohlcvData = historicalDataService.getOHLCVData(
                symbol.toUpperCase(), interval, startTime, endTime);
            return ResponseEntity.ok(ohlcvData);
        } catch (Exception e) {
            logger.error("Error getting OHLCV data for {}", symbol, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get price statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getPriceStatistics(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "24h") String timeRange) {
        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = calculateStartTimeFromRange(endTime, timeRange);
            
            Map<String, Object> statistics = historicalDataService.getPriceStatistics(
                symbol.toUpperCase(), startTime, endTime);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Error getting price statistics for {}", symbol, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get latest prices for all symbols
     */
    @GetMapping("/latest")
    public ResponseEntity<List<PriceData>> getLatestPrices() {
        try {
            List<PriceData> latestPrices = historicalDataService.getLatestPricesForAllSymbols();
            return ResponseEntity.ok(latestPrices);
        } catch (Exception e) {
            logger.error("Error getting latest prices", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Helper method to calculate start time based on interval and limit
     */
    private LocalDateTime calculateStartTime(LocalDateTime endTime, String interval, int limit) {
        switch (interval.toLowerCase()) {
            case "1m":
                return endTime.minusMinutes(limit);
            case "5m":
                return endTime.minusMinutes(limit * 5);
            case "15m":
                return endTime.minusMinutes(limit * 15);
            case "30m":
                return endTime.minusMinutes(limit * 30);
            case "1h":
                return endTime.minusHours(limit);
            case "4h":
                return endTime.minusHours(limit * 4);
            case "1d":
                return endTime.minusDays(limit);
            case "1w":
                return endTime.minusWeeks(limit);
            default:
                return endTime.minusHours(limit);
        }
    }

    /**
     * Helper method to calculate start time from time range string
     */
    private LocalDateTime calculateStartTimeFromRange(LocalDateTime endTime, String timeRange) {
        switch (timeRange.toLowerCase()) {
            case "1h":
                return endTime.minusHours(1);
            case "4h":
                return endTime.minusHours(4);
            case "12h":
                return endTime.minusHours(12);
            case "24h":
                return endTime.minusDays(1);
            case "7d":
                return endTime.minusWeeks(1);
            case "30d":
                return endTime.minusMonths(1);
            default:
                return endTime.minusDays(1);
        }
    }
}
