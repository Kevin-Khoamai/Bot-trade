package com.cryptotrading.dataacquisition.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for market data that will be sent via Kafka
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDataDto {

    private String exchange;
    private String symbol;
    private LocalDateTime timestamp;
    
    @JsonProperty("open")
    private BigDecimal openPrice;
    
    @JsonProperty("high")
    private BigDecimal highPrice;
    
    @JsonProperty("low")
    private BigDecimal lowPrice;
    
    @JsonProperty("close")
    private BigDecimal closePrice;
    
    private BigDecimal volume;
    private String dataType; // KLINE, TRADE, TICKER
    private String source; // REST, WEBSOCKET

    /**
     * Create from Binance kline data
     */
    public static MarketDataDto fromBinanceKline(String symbol, Object[] klineData) {
        return MarketDataDto.builder()
                .exchange("BINANCE")
                .symbol(symbol)
                .timestamp(LocalDateTime.ofEpochSecond(Long.parseLong(klineData[0].toString()) / 1000, 0, java.time.ZoneOffset.UTC))
                .openPrice(new BigDecimal(klineData[1].toString()))
                .highPrice(new BigDecimal(klineData[2].toString()))
                .lowPrice(new BigDecimal(klineData[3].toString()))
                .closePrice(new BigDecimal(klineData[4].toString()))
                .volume(new BigDecimal(klineData[5].toString()))
                .dataType("KLINE")
                .source("REST")
                .build();
    }

    /**
     * Create from Coinbase candle data
     */
    public static MarketDataDto fromCoinbaseCandle(String symbol, Object[] candleData) {
        return MarketDataDto.builder()
                .exchange("COINBASE")
                .symbol(symbol)
                .timestamp(LocalDateTime.ofEpochSecond(Long.parseLong(candleData[0].toString()), 0, java.time.ZoneOffset.UTC))
                .lowPrice(new BigDecimal(candleData[1].toString()))
                .highPrice(new BigDecimal(candleData[2].toString()))
                .openPrice(new BigDecimal(candleData[3].toString()))
                .closePrice(new BigDecimal(candleData[4].toString()))
                .volume(new BigDecimal(candleData[5].toString()))
                .dataType("CANDLE")
                .source("REST")
                .build();
    }
}
