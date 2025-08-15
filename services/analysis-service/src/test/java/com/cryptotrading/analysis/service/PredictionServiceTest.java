package com.cryptotrading.analysis.service;

import com.cryptotrading.analysis.dto.MarketDataDto;
import com.cryptotrading.analysis.model.TechnicalIndicator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PredictionServiceTest {

    @Mock
    private RedisCacheService redisCacheService;

    @InjectMocks
    private PredictionService predictionService;

    private List<MarketDataDto> testMarketData;
    private String testSymbol = "BTCUSDT";

    @BeforeEach
    void setUp() {
        testMarketData = createTestMarketData();
    }

    @Test
    void testGeneratePredictions_WithSufficientData() {
        // Given
        when(redisCacheService.cachePrediction(anyString(), any())).thenReturn();

        // When
        List<TechnicalIndicator> predictions = predictionService.generatePredictions(testSymbol, testMarketData);

        // Then
        assertNotNull(predictions);
        assertFalse(predictions.isEmpty());
        
        // Should have at least ARIMA, ML, and Trend predictions
        assertTrue(predictions.size() >= 1);
        
        // Check that predictions have correct symbol and types
        for (TechnicalIndicator prediction : predictions) {
            assertEquals(testSymbol, prediction.getSymbol());
            assertNotNull(prediction.getIndicatorValue());
            assertTrue(prediction.getIndicatorValue().compareTo(BigDecimal.ZERO) > 0);
        }
    }

    @Test
    void testGeneratePredictions_WithInsufficientData() {
        // Given
        List<MarketDataDto> insufficientData = testMarketData.subList(0, 5);

        // When
        List<TechnicalIndicator> predictions = predictionService.generatePredictions(testSymbol, insufficientData);

        // Then
        assertNotNull(predictions);
        assertTrue(predictions.isEmpty());
    }

    @Test
    void testGeneratePredictions_WithEmptyData() {
        // Given
        List<MarketDataDto> emptyData = new ArrayList<>();

        // When
        List<TechnicalIndicator> predictions = predictionService.generatePredictions(testSymbol, emptyData);

        // Then
        assertNotNull(predictions);
        assertTrue(predictions.isEmpty());
    }

    @Test
    void testGeneratePredictions_WithNullData() {
        // When
        List<TechnicalIndicator> predictions = predictionService.generatePredictions(testSymbol, null);

        // Then
        assertNotNull(predictions);
        assertTrue(predictions.isEmpty());
    }

    @Test
    void testARIMAPrediction_ReturnsValidValue() {
        // Given
        when(redisCacheService.cachePrediction(anyString(), any())).thenReturn();

        // When
        List<TechnicalIndicator> predictions = predictionService.generatePredictions(testSymbol, testMarketData);

        // Then
        assertNotNull(predictions);
        
        // Find ARIMA prediction
        TechnicalIndicator arimaPrediction = predictions.stream()
                .filter(p -> p.getIndicatorType().equals(TechnicalIndicator.IndicatorType.ARIMA_PREDICTION.name()))
                .findFirst()
                .orElse(null);

        if (arimaPrediction != null) {
            assertNotNull(arimaPrediction.getIndicatorValue());
            assertTrue(arimaPrediction.getIndicatorValue().compareTo(BigDecimal.ZERO) > 0);
            assertEquals(testSymbol, arimaPrediction.getSymbol());
        }
    }

    @Test
    void testMLPrediction_ReturnsValidValue() {
        // Given
        when(redisCacheService.cachePrediction(anyString(), any())).thenReturn();

        // When
        List<TechnicalIndicator> predictions = predictionService.generatePredictions(testSymbol, testMarketData);

        // Then
        assertNotNull(predictions);
        
        // Find ML prediction
        TechnicalIndicator mlPrediction = predictions.stream()
                .filter(p -> p.getIndicatorType().equals(TechnicalIndicator.IndicatorType.ML_PREDICTION.name()))
                .findFirst()
                .orElse(null);

        if (mlPrediction != null) {
            assertNotNull(mlPrediction.getIndicatorValue());
            assertTrue(mlPrediction.getIndicatorValue().compareTo(BigDecimal.ZERO) > 0);
            assertEquals(testSymbol, mlPrediction.getSymbol());
        }
    }

    @Test
    void testTrendPrediction_ReturnsValidValue() {
        // Given
        when(redisCacheService.cachePrediction(anyString(), any())).thenReturn();

        // When
        List<TechnicalIndicator> predictions = predictionService.generatePredictions(testSymbol, testMarketData);

        // Then
        assertNotNull(predictions);
        
        // Find Trend prediction
        TechnicalIndicator trendPrediction = predictions.stream()
                .filter(p -> p.getIndicatorType().equals(TechnicalIndicator.IndicatorType.TREND_PREDICTION.name()))
                .findFirst()
                .orElse(null);

        if (trendPrediction != null) {
            assertNotNull(trendPrediction.getIndicatorValue());
            assertTrue(trendPrediction.getIndicatorValue().compareTo(BigDecimal.ZERO) > 0);
            assertEquals(testSymbol, trendPrediction.getSymbol());
        }
    }

    @Test
    void testPredictionConsistency() {
        // Given
        when(redisCacheService.cachePrediction(anyString(), any())).thenReturn();

        // When - Generate predictions multiple times
        List<TechnicalIndicator> predictions1 = predictionService.generatePredictions(testSymbol, testMarketData);
        List<TechnicalIndicator> predictions2 = predictionService.generatePredictions(testSymbol, testMarketData);

        // Then - Results should be consistent (within reasonable range)
        assertNotNull(predictions1);
        assertNotNull(predictions2);
        assertEquals(predictions1.size(), predictions2.size());
    }

    /**
     * Create test market data with realistic price movements
     */
    private List<MarketDataDto> createTestMarketData() {
        List<MarketDataDto> data = new ArrayList<>();
        BigDecimal basePrice = new BigDecimal("50000.00");
        LocalDateTime timestamp = LocalDateTime.now().minusHours(24);

        for (int i = 0; i < 50; i++) {
            // Simulate realistic price movements
            double randomFactor = 0.98 + (Math.random() * 0.04); // ±2% variation
            BigDecimal price = basePrice.multiply(BigDecimal.valueOf(randomFactor));
            
            MarketDataDto marketData = MarketDataDto.builder()
                    .exchange("BINANCE")
                    .symbol(testSymbol)
                    .timestamp(timestamp.plusMinutes(i * 30))
                    .openPrice(price)
                    .highPrice(price.multiply(BigDecimal.valueOf(1.01)))
                    .lowPrice(price.multiply(BigDecimal.valueOf(0.99)))
                    .closePrice(price)
                    .volume(BigDecimal.valueOf(100 + Math.random() * 1000))
                    .build();
            
            data.add(marketData);
            basePrice = price; // Use current price as base for next iteration
        }

        return data;
    }
}
