package com.cryptotrading.dataacquisition.client;

import com.cryptotrading.dataacquisition.dto.MarketDataDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * REST client for Coinbase Pro API
 * Fetches historical and current market data
 */
@Component
@Slf4j
public class CoinbaseRestClient {

    private final WebClient webClient;
    private final String baseUrl;

    public CoinbaseRestClient(@Value("${exchange.coinbase.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

    /**
     * Fetch candle data for a product
     * 
     * @param productId Trading pair (e.g., BTC-USD)
     * @param granularity Granularity in seconds (60, 300, 900, 3600, 21600, 86400)
     * @param start Start time
     * @param end End time
     * @return Flux of market data
     */
    @CircuitBreaker(name = "coinbase-api", fallbackMethod = "fallbackCandleData")
    public Flux<MarketDataDto> getCandleData(String productId, int granularity, 
                                           LocalDateTime start, LocalDateTime end) {
        log.debug("Fetching candle data for product: {}, granularity: {}", productId, granularity);
        
        String startStr = start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endStr = end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/products/{product-id}/candles")
                        .queryParam("start", startStr)
                        .queryParam("end", endStr)
                        .queryParam("granularity", granularity)
                        .build(productId))
                .retrieve()
                .bodyToMono(Object[][].class)
                .flatMapMany(candles -> Flux.fromArray(candles))
                .map(candle -> MarketDataDto.fromCoinbaseCandle(productId, candle))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(throwable -> !(throwable instanceof IllegalArgumentException)))
                .doOnError(error -> log.error("Error fetching candle data for {}: {}", productId, error.getMessage()))
                .onErrorResume(error -> {
                    log.warn("Failed to fetch candle data for {}, returning empty flux", productId);
                    return Flux.empty();
                });
    }

    /**
     * Fetch current ticker for a product
     * 
     * @param productId Trading pair
     * @return Mono of ticker data
     */
    @CircuitBreaker(name = "coinbase-api", fallbackMethod = "fallbackTickerData")
    public Mono<Object> getTickerData(String productId) {
        log.debug("Fetching ticker data for product: {}", productId);
        
        return webClient.get()
                .uri("/products/{product-id}/ticker", productId)
                .retrieve()
                .bodyToMono(Object.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .doOnError(error -> log.error("Error fetching ticker data for {}: {}", productId, error.getMessage()));
    }

    /**
     * Fetch 24hr stats for a product
     * 
     * @param productId Trading pair
     * @return Mono of stats data
     */
    @CircuitBreaker(name = "coinbase-api", fallbackMethod = "fallbackStatsData")
    public Mono<Object> getStatsData(String productId) {
        log.debug("Fetching stats data for product: {}", productId);
        
        return webClient.get()
                .uri("/products/{product-id}/stats", productId)
                .retrieve()
                .bodyToMono(Object.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .doOnError(error -> log.error("Error fetching stats data for {}: {}", productId, error.getMessage()));
    }

    /**
     * Fetch all available products
     * 
     * @return Mono of products data
     */
    @CircuitBreaker(name = "coinbase-api", fallbackMethod = "fallbackProductsData")
    public Mono<Object> getProducts() {
        log.debug("Fetching products data");
        
        return webClient.get()
                .uri("/products")
                .retrieve()
                .bodyToMono(Object.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .doOnError(error -> log.error("Error fetching products data: {}", error.getMessage()));
    }

    /**
     * Convert Binance symbol to Coinbase product ID
     * Example: BTCUSDT -> BTC-USD
     */
    public String convertSymbolToProductId(String binanceSymbol) {
        // Simple conversion logic - can be enhanced
        if (binanceSymbol.endsWith("USDT")) {
            String base = binanceSymbol.substring(0, binanceSymbol.length() - 4);
            return base + "-USD";
        }
        if (binanceSymbol.endsWith("BTC")) {
            String base = binanceSymbol.substring(0, binanceSymbol.length() - 3);
            return base + "-BTC";
        }
        if (binanceSymbol.endsWith("ETH")) {
            String base = binanceSymbol.substring(0, binanceSymbol.length() - 3);
            return base + "-ETH";
        }
        return binanceSymbol; // Return as-is if no conversion rule matches
    }

    // Fallback methods for circuit breaker

    public Flux<MarketDataDto> fallbackCandleData(String productId, int granularity, 
                                                LocalDateTime start, LocalDateTime end, Exception ex) {
        log.warn("Circuit breaker activated for candle data: {}", ex.getMessage());
        return Flux.empty();
    }

    public Mono<Object> fallbackTickerData(String productId, Exception ex) {
        log.warn("Circuit breaker activated for ticker data: {}", ex.getMessage());
        return Mono.empty();
    }

    public Mono<Object> fallbackStatsData(String productId, Exception ex) {
        log.warn("Circuit breaker activated for stats data: {}", ex.getMessage());
        return Mono.empty();
    }

    public Mono<Object> fallbackProductsData(Exception ex) {
        log.warn("Circuit breaker activated for products data: {}", ex.getMessage());
        return Mono.empty();
    }
}
