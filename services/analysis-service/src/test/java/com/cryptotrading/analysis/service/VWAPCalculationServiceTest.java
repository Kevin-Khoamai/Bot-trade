package com.cryptotrading.analysis.service;

import com.cryptotrading.analysis.dto.MarketDataDto;
import com.cryptotrading.analysis.model.TechnicalIndicator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class VWAPCalculationServiceTest {

    @InjectMocks
    private VWAPCalculationService vwapCalculationService;

    private List<MarketDataDto> testMarketData;
    private String testSymbol = "BTCUSDT";

    @BeforeEach
    void setUp() {
        testMarketData = createTestMarketData();
    }

    @Test
    void testCalculateVWAP_WithValidData() {
        // When
        TechnicalIndicator vwapIndicator = vwapCalculationService.calculateVWAP(testSymbol, testMarketData);

        // Then
        assertNotNull(vwapIndicator);
        assertEquals(testSymbol, vwapIndicator.getSymbol());
        assertEquals(TechnicalIndicator.IndicatorType.VWAP.name(), vwapIndicator.getIndicatorType());
        assertNotNull(vwapIndicator.getIndicatorValue());
        assertTrue(vwapIndicator.getIndicatorValue().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testCalculateVWAP_WithEmptyData() {
        // Given
        List<MarketDataDto> emptyData = new ArrayList<>();

        // When
        TechnicalIndicator vwapIndicator = vwapCalculationService.calculateVWAP(testSymbol, emptyData);

        // Then
        assertNull(vwapIndicator);
    }

    @Test
    void testCalculateVWAP_ManualCalculation() {
        // Given - Simple test data for manual verification
        List<MarketDataDto> simpleData = createSimpleTestData();

        // When
        TechnicalIndicator vwapIndicator = vwapCalculationService.calculateVWAP(testSymbol, simpleData);

        // Then
        assertNotNull(vwapIndicator);
        
        // Manual calculation:
        // Data point 1: price=100, volume=10 -> volume*price = 1000
        // Data point 2: price=110, volume=20 -> volume*price = 2200
        // Data point 3: price=105, volume=15 -> volume*price = 1575
        // Total volume*price = 4775, Total volume = 45
        // VWAP = 4775/45 = 106.111...
        
        BigDecimal expectedVWAP = new BigDecimal("106.11111111");
        assertEquals(0, expectedVWAP.compareTo(vwapIndicator.getIndicatorValue()));
    }

    @Test
    void testCalculateIntradayVWAP() {
        // Given - Data for current day
        List<MarketDataDto> todayData = createTodayTestData();

        // When
        TechnicalIndicator intradayVWAP = vwapCalculationService.calculateIntradayVWAP(testSymbol, todayData);

        // Then
        assertNotNull(intradayVWAP);
        assertEquals(testSymbol, intradayVWAP.getSymbol());
        assertEquals(TechnicalIndicator.IndicatorType.VWAP.name(), intradayVWAP.getIndicatorType());
        assertEquals(Integer.valueOf(1440), intradayVWAP.getPeriod()); // 1 day in minutes
    }

    @Test
    void testCalculateRollingVWAP() {
        // Given
        int[] periods = {5, 10, 20};

        // When
        List<TechnicalIndicator> rollingVWAPs = vwapCalculationService.calculateRollingVWAP(testSymbol, testMarketData, periods);

        // Then
        assertNotNull(rollingVWAPs);
        assertEquals(periods.length, rollingVWAPs.size());
        
        for (int i = 0; i < periods.length; i++) {
            TechnicalIndicator vwap = rollingVWAPs.get(i);
            assertEquals(testSymbol, vwap.getSymbol());
            assertEquals(TechnicalIndicator.IndicatorType.VWAP.name(), vwap.getIndicatorType());
            assertEquals(Integer.valueOf(periods[i]), vwap.getPeriod());
        }
    }

    @Test
    void testCalculateVWAPBands() {
        // Given
        double standardDeviations = 2.0;

        // When
        VWAPCalculationService.VWAPBands bands = vwapCalculationService.calculateVWAPBands(testSymbol, testMarketData, standardDeviations);

        // Then
        assertNotNull(bands);
        assertNotNull(bands.getVwap());
        assertNotNull(bands.getUpperBand());
        assertNotNull(bands.getLowerBand());
        assertNotNull(bands.getStandardDeviation());
        
        // Upper band should be greater than VWAP
        assertTrue(bands.getUpperBand().compareTo(bands.getVwap()) > 0);
        
        // Lower band should be less than VWAP
        assertTrue(bands.getLowerBand().compareTo(bands.getVwap()) < 0);
        
        // Standard deviation should be positive
        assertTrue(bands.getStandardDeviation().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testUpdateVWAPCache() {
        // Given
        MarketDataDto newData = testMarketData.get(0);

        // When
        vwapCalculationService.updateVWAPCache(testSymbol, newData);
        BigDecimal cachedVWAP = vwapCalculationService.getCachedVWAP(testSymbol);

        // Then
        assertNotNull(cachedVWAP);
        assertTrue(cachedVWAP.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testCalculateVWAPDeviation() {
        // Given
        vwapCalculationService.updateVWAPCache(testSymbol, testMarketData.get(0));
        BigDecimal currentPrice = new BigDecimal("105.00");

        // When
        BigDecimal deviation = vwapCalculationService.calculateVWAPDeviation(testSymbol, currentPrice);

        // Then
        assertNotNull(deviation);
        // Deviation should be a percentage
        assertTrue(deviation.abs().compareTo(new BigDecimal("100")) < 0); // Should be less than 100%
    }

    @Test
    void testVWAPWithZeroVolume() {
        // Given - Data with zero volume
        List<MarketDataDto> zeroVolumeData = createZeroVolumeTestData();

        // When
        TechnicalIndicator vwapIndicator = vwapCalculationService.calculateVWAP(testSymbol, zeroVolumeData);

        // Then
        assertNull(vwapIndicator);
    }

    /**
     * Create test market data
     */
    private List<MarketDataDto> createTestMarketData() {
        List<MarketDataDto> data = new ArrayList<>();
        BigDecimal basePrice = new BigDecimal("100.00");
        LocalDateTime timestamp = LocalDateTime.now().minusHours(1);

        for (int i = 0; i < 30; i++) {
            BigDecimal price = basePrice.add(BigDecimal.valueOf(i * 0.5));
            BigDecimal volume = BigDecimal.valueOf(100 + i * 10);
            
            MarketDataDto marketData = MarketDataDto.builder()
                    .exchange("BINANCE")
                    .symbol(testSymbol)
                    .timestamp(timestamp.plusMinutes(i * 2))
                    .openPrice(price)
                    .highPrice(price.add(BigDecimal.ONE))
                    .lowPrice(price.subtract(BigDecimal.ONE))
                    .closePrice(price)
                    .volume(volume)
                    .build();
            
            data.add(marketData);
        }

        return data;
    }

    /**
     * Create simple test data for manual verification
     */
    private List<MarketDataDto> createSimpleTestData() {
        List<MarketDataDto> data = new ArrayList<>();
        LocalDateTime timestamp = LocalDateTime.now();

        // Data point 1: price=100, volume=10
        data.add(MarketDataDto.builder()
                .exchange("BINANCE")
                .symbol(testSymbol)
                .timestamp(timestamp)
                .openPrice(new BigDecimal("100"))
                .highPrice(new BigDecimal("100"))
                .lowPrice(new BigDecimal("100"))
                .closePrice(new BigDecimal("100"))
                .volume(new BigDecimal("10"))
                .build());

        // Data point 2: price=110, volume=20
        data.add(MarketDataDto.builder()
                .exchange("BINANCE")
                .symbol(testSymbol)
                .timestamp(timestamp.plusMinutes(1))
                .openPrice(new BigDecimal("110"))
                .highPrice(new BigDecimal("110"))
                .lowPrice(new BigDecimal("110"))
                .closePrice(new BigDecimal("110"))
                .volume(new BigDecimal("20"))
                .build());

        // Data point 3: price=105, volume=15
        data.add(MarketDataDto.builder()
                .exchange("BINANCE")
                .symbol(testSymbol)
                .timestamp(timestamp.plusMinutes(2))
                .openPrice(new BigDecimal("105"))
                .highPrice(new BigDecimal("105"))
                .lowPrice(new BigDecimal("105"))
                .closePrice(new BigDecimal("105"))
                .volume(new BigDecimal("15"))
                .build());

        return data;
    }

    /**
     * Create test data for current day
     */
    private List<MarketDataDto> createTodayTestData() {
        List<MarketDataDto> data = new ArrayList<>();
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

        for (int i = 0; i < 10; i++) {
            BigDecimal price = new BigDecimal("100").add(BigDecimal.valueOf(i));
            
            MarketDataDto marketData = MarketDataDto.builder()
                    .exchange("BINANCE")
                    .symbol(testSymbol)
                    .timestamp(startOfDay.plusHours(i))
                    .openPrice(price)
                    .highPrice(price.add(BigDecimal.ONE))
                    .lowPrice(price.subtract(BigDecimal.ONE))
                    .closePrice(price)
                    .volume(BigDecimal.valueOf(100))
                    .build();
            
            data.add(marketData);
        }

        return data;
    }

    /**
     * Create test data with zero volume
     */
    private List<MarketDataDto> createZeroVolumeTestData() {
        List<MarketDataDto> data = new ArrayList<>();
        LocalDateTime timestamp = LocalDateTime.now();

        MarketDataDto marketData = MarketDataDto.builder()
                .exchange("BINANCE")
                .symbol(testSymbol)
                .timestamp(timestamp)
                .openPrice(new BigDecimal("100"))
                .highPrice(new BigDecimal("100"))
                .lowPrice(new BigDecimal("100"))
                .closePrice(new BigDecimal("100"))
                .volume(BigDecimal.ZERO)
                .build();
        
        data.add(marketData);
        return data;
    }
}
