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
import java.util.List;

/**
 * REST client for Binance API
 * Fetches historical and current market data
 */
@Component
@Slf4j
public class BinanceRestClient {

    private final WebClient webClient;
    private final String baseUrl;

    public BinanceRestClient(@Value("${exchange.binance.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

    /**
     * Fetch kline/candlestick data for a symbol
     * 
     * @param symbol Trading pair symbol (e.g., BTCUSDT)
     * @param interval Time interval (1m, 5m, 15m, 1h, 4h, 1d)
     * @param limit Number of data points to fetch (max 1000)
     * @return Flux of market data
     */
    @CircuitBreaker(name = "binance-api", fallbackMethod = "fallbackKlineData")
    public Flux<MarketDataDto> getKlineData(String symbol, String interval, int limit) {
        log.debug("Fetching kline data for symbol: {}, interval: {}, limit: {}", symbol, interval, limit);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v3/klines")
                        .queryParam("symbol", symbol)
                        .queryParam("interval", interval)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .bodyToMono(Object[][].class)
                .flatMapMany(klines -> Flux.fromArray(klines))
                .map(kline -> MarketDataDto.fromBinanceKline(symbol, kline))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(throwable -> !(throwable instanceof IllegalArgumentException)))
                .doOnError(error -> log.error("Error fetching kline data for {}: {}", symbol, error.getMessage()))
                .onErrorResume(error -> {
                    log.warn("Failed to fetch kline data for {}, returning empty flux", symbol);
                    return Flux.empty();
                });
    }

    /**
     * Fetch current 24hr ticker statistics
     * 
     * @param symbol Trading pair symbol
     * @return Mono of ticker data
     */
    @CircuitBreaker(name = "binance-api", fallbackMethod = "fallbackTickerData")
    public Mono<Object> getTickerData(String symbol) {
        log.debug("Fetching ticker data for symbol: {}", symbol);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v3/ticker/24hr")
                        .queryParam("symbol", symbol)
                        .build())
                .retrieve()
                .bodyToMono(Object.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .doOnError(error -> log.error("Error fetching ticker data for {}: {}", symbol, error.getMessage()));
    }

    /**
     * Fetch current price for a symbol
     * 
     * @param symbol Trading pair symbol
     * @return Mono of price data
     */
    @CircuitBreaker(name = "binance-api", fallbackMethod = "fallbackPriceData")
    public Mono<Object> getCurrentPrice(String symbol) {
        log.debug("Fetching current price for symbol: {}", symbol);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v3/ticker/price")
                        .queryParam("symbol", symbol)
                        .build())
                .retrieve()
                .bodyToMono(Object.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .doOnError(error -> log.error("Error fetching price data for {}: {}", symbol, error.getMessage()));
    }

    /**
     * Fetch exchange info to get available symbols and their details
     * 
     * @return Mono of exchange info
     */
    @CircuitBreaker(name = "binance-api", fallbackMethod = "fallbackExchangeInfo")
    public Mono<Object> getExchangeInfo() {
        log.debug("Fetching exchange info");
        
        return webClient.get()
                .uri("/api/v3/exchangeInfo")
                .retrieve()
                .bodyToMono(Object.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .doOnError(error -> log.error("Error fetching exchange info: {}", error.getMessage()));
    }

    // Fallback methods for circuit breaker

    public Flux<MarketDataDto> fallbackKlineData(String symbol, String interval, int limit, Exception ex) {
        log.warn("Circuit breaker activated for kline data: {}", ex.getMessage());
        return Flux.empty();
    }

    public Mono<Object> fallbackTickerData(String symbol, Exception ex) {
        log.warn("Circuit breaker activated for ticker data: {}", ex.getMessage());
        return Mono.empty();
    }

    public Mono<Object> fallbackPriceData(String symbol, Exception ex) {
        log.warn("Circuit breaker activated for price data: {}", ex.getMessage());
        return Mono.empty();
    }

    public Mono<Object> fallbackExchangeInfo(Exception ex) {
        log.warn("Circuit breaker activated for exchange info: {}", ex.getMessage());
        return Mono.empty();
    }
}
