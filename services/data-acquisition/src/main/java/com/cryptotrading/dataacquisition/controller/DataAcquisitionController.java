package com.cryptotrading.dataacquisition.controller;

import com.cryptotrading.dataacquisition.model.CryptoPrice;
import com.cryptotrading.dataacquisition.repository.CryptoPriceRepository;
import com.cryptotrading.dataacquisition.service.DataAcquisitionService;
import com.cryptotrading.dataacquisition.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for data acquisition operations
 */
@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
@Slf4j
public class DataAcquisitionController {

    private final DataAcquisitionService dataAcquisitionService;
    private final CryptoPriceRepository cryptoPriceRepository;
    private final RedisCacheService redisCacheService;

    /**
     * Trigger manual data fetch
     */
    @PostMapping("/fetch")
    public ResponseEntity<String> triggerDataFetch() {
        log.info("Manual data fetch triggered via API");
        dataAcquisitionService.triggerDataFetch();
        return ResponseEntity.ok("Data fetch triggered successfully");
    }

    /**
     * Fetch data for specific symbol
     */
    @PostMapping("/fetch/{symbol}")
    public ResponseEntity<String> fetchSymbolData(@PathVariable String symbol) {
        log.info("Manual data fetch triggered for symbol: {}", symbol);
        dataAcquisitionService.fetchAndProcessMarketData(symbol);
        return ResponseEntity.ok("Data fetch triggered for symbol: " + symbol);
    }

    /**
     * Get latest price for a symbol
     */
    @GetMapping("/price/{exchange}/{symbol}")
    public ResponseEntity<CryptoPrice> getLatestPrice(
            @PathVariable String exchange,
            @PathVariable String symbol) {
        
        Optional<CryptoPrice> price = dataAcquisitionService.getLatestPrice(exchange, symbol);
        return price.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get historical prices for a symbol within date range
     */
    @GetMapping("/prices/{symbol}")
    public ResponseEntity<List<CryptoPrice>> getHistoricalPrices(
            @PathVariable String symbol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        List<CryptoPrice> prices = cryptoPriceRepository
                .findBySymbolAndTimestampBetween(symbol, startTime, endTime);
        
        return ResponseEntity.ok(prices);
    }

    /**
     * Get recent prices for a symbol
     */
    @GetMapping("/prices/{symbol}/recent")
    public ResponseEntity<List<CryptoPrice>> getRecentPrices(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit) {
        
        List<CryptoPrice> prices = cryptoPriceRepository.findRecentBySymbol(symbol, limit);
        return ResponseEntity.ok(prices);
    }

    /**
     * Get all available symbols for an exchange
     */
    @GetMapping("/symbols/{exchange}")
    public ResponseEntity<List<String>> getSymbolsByExchange(@PathVariable String exchange) {
        List<String> symbols = cryptoPriceRepository.findDistinctSymbolsByExchange(exchange);
        return ResponseEntity.ok(symbols);
    }

    /**
     * Get all exchanges for a symbol
     */
    @GetMapping("/exchanges/{symbol}")
    public ResponseEntity<List<String>> getExchangesBySymbol(@PathVariable String symbol) {
        List<String> exchanges = cryptoPriceRepository.findDistinctExchangesBySymbol(symbol);
        return ResponseEntity.ok(exchanges);
    }

    /**
     * Get cached real-time price
     */
    @GetMapping("/realtime/{exchange}/{symbol}")
    public ResponseEntity<Object> getRealtimePrice(
            @PathVariable String exchange,
            @PathVariable String symbol) {
        
        Optional<Object> cachedPrice = redisCacheService.getCachedRealtimePrice(exchange, symbol);
        return cachedPrice.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get cached ticker data
     */
    @GetMapping("/ticker/{exchange}/{symbol}")
    public ResponseEntity<Object> getTickerData(
            @PathVariable String exchange,
            @PathVariable String symbol) {
        
        Optional<Object> cachedTicker = redisCacheService.getCachedTickerData(exchange, symbol);
        return cachedTicker.map(ResponseEntity::ok)
                          .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Trigger current price fetch
     */
    @PostMapping("/fetch/current-prices")
    public ResponseEntity<String> fetchCurrentPrices() {
        log.info("Current prices fetch triggered via API");
        dataAcquisitionService.fetchCurrentPrices();
        return ResponseEntity.ok("Current prices fetch triggered successfully");
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Data Acquisition Service is healthy");
    }

    /**
     * Get data statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Object> getDataStats() {
        long totalRecords = cryptoPriceRepository.count();
        List<String> allSymbols = cryptoPriceRepository.findDistinctSymbolsByExchange("BINANCE");
        
        return ResponseEntity.ok(new DataStats(totalRecords, allSymbols.size()));
    }

    /**
     * Data statistics DTO
     */
    public record DataStats(long totalRecords, int symbolCount) {}
}
