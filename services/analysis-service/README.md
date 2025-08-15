# Analysis Service

The Analysis Service processes market data from Kafka streams to compute technical indicators, generate trading signals, and provide price predictions. It uses the Ta4j library for technical analysis and caches results in Redis for high-performance access.

## Features

- **Technical Indicators**: RSI, MACD, Moving Averages, Bollinger Bands, Stochastic, ATR, ADX, OBV
- **Real-time Processing**: Consumes market data from Kafka streams
- **Signal Generation**: Generates BUY/SELL/HOLD signals based on indicator analysis
- **Caching**: Redis-based caching for fast indicator retrieval
- **Batch Processing**: Buffers market data for efficient batch analysis
- **Alert System**: Publishes alerts for significant market conditions
- **REST API**: Comprehensive API for accessing analysis results

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Kafka Topics  │    │   Kafka Topics  │    │   Kafka Topics  │
│ binance-trades  │    │ coinbase-trades │    │aggregated-data  │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Analysis Service                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ Market Data     │  │ Indicator       │  │ Signal          │ │
│  │ Consumer        │  │ Calculator      │  │ Generator       │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ Data Buffer     │  │ Analysis        │  │ Alert           │ │
│  │ Manager         │  │ Result Service  │  │ Publisher       │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────┬───────────────────────┬───────────────────────┬───────┘
          │                       │                       │
          ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   PostgreSQL    │    │      Redis      │    │      Kafka      │
│   (Storage)     │    │    (Cache)      │    │   (Publish)     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Technical Indicators

### Trend Indicators
- **SMA/EMA**: Simple and Exponential Moving Averages (5, 10, 20, 50, 100, 200 periods)
- **MACD**: Moving Average Convergence Divergence with signal line and histogram
- **ADX**: Average Directional Index for trend strength

### Momentum Indicators
- **RSI**: Relative Strength Index (14-period)
- **Stochastic**: Stochastic Oscillator (%K and %D)
- **Williams %R**: Williams Percent Range

### Volatility Indicators
- **Bollinger Bands**: Upper, Middle, and Lower bands
- **ATR**: Average True Range (14-period)

### Volume Indicators
- **OBV**: On Balance Volume
- **Volume SMA**: Volume moving averages

## API Endpoints

### Health Check
- `GET /api/analysis/health` - Service health status

### Indicators
- `GET /api/analysis/indicators/{symbol}` - Get latest indicators for symbol
- `GET /api/analysis/indicators/{symbol}/{indicatorType}` - Get specific indicator
- `GET /api/analysis/indicators/{symbol}/{indicatorType}/history` - Historical indicators
- `GET /api/analysis/indicators/{symbol}/{indicatorType}/recent` - Recent indicators

### Analysis Results
- `GET /api/analysis/result/{symbol}` - Comprehensive analysis result
- `GET /api/analysis/signal/{symbol}` - Trading signal for symbol
- `GET /api/analysis/prediction/{symbol}` - Price prediction for symbol

### Metadata
- `GET /api/analysis/symbols` - All symbols with indicators
- `GET /api/analysis/indicators/{symbol}/types` - Indicator types for symbol

### Monitoring
- `GET /api/analysis/buffers` - Buffer status for monitoring
- `POST /api/analysis/buffers/{symbol}/process` - Force process buffer
- `DELETE /api/analysis/buffers/{symbol}` - Clear buffer
- `GET /api/analysis/cache/stats` - Cache statistics
- `DELETE /api/analysis/cache/{symbol}` - Clear cache for symbol
- `GET /api/analysis/stats` - Analysis statistics

## Configuration

### Key Configuration Properties

```yaml
# Analysis Configuration
analysis:
  indicators:
    ma-periods: [5, 10, 20, 50, 100, 200]
    rsi-period: 14
    macd:
      fast-period: 12
      slow-period: 26
      signal-period: 9
    bb:
      period: 20
      multiplier: 2.0
  
  cache:
    indicator-ttl: 300  # 5 minutes
    prediction-ttl: 600  # 10 minutes

# Kafka Topics
kafka:
  topics:
    binance-trades: binance-trades
    coinbase-trades: coinbase-trades
    aggregated-data: aggregated-market-data
    indicators: technical-indicators
    predictions: price-predictions
    analysis-alerts: analysis-alerts
```

## Data Flow

1. **Market Data Consumption**: Consumes real-time market data from Kafka topics
2. **Data Buffering**: Buffers data points for efficient batch processing
3. **Indicator Calculation**: Uses Ta4j library to calculate technical indicators
4. **Signal Generation**: Analyzes indicators to generate trading signals
5. **Result Caching**: Caches results in Redis for fast access
6. **Database Storage**: Persists indicators in PostgreSQL
7. **Publishing**: Publishes results and alerts to Kafka topics

## Signal Generation Logic

### Trend Signals
- **BULLISH**: Price above moving averages, MACD above signal line
- **BEARISH**: Price below moving averages, MACD below signal line
- **NEUTRAL**: Mixed or unclear trend signals

### Momentum Signals
- **OVERBOUGHT**: RSI > 70, Stochastic > 80
- **OVERSOLD**: RSI < 30, Stochastic < 20
- **NEUTRAL**: RSI between 30-70

### Overall Signals
- **STRONG_BUY**: Bullish trend + Oversold momentum
- **BUY**: Bullish trend
- **STRONG_SELL**: Bearish trend + Overbought momentum
- **SELL**: Bearish trend
- **HOLD**: Neutral or mixed signals

## Building and Running

### Prerequisites
- Java 17+
- Maven 3.6+
- PostgreSQL, Redis, and Kafka running
- Data Acquisition Service running (for market data)

### Build
```bash
mvn clean package
```

### Run with Docker
```bash
docker build -t crypto-analysis-service .
docker run -p 8082:8082 crypto-analysis-service
```

### Run Locally
```bash
# Ensure infrastructure is running
docker-compose up -d postgres redis kafka

# Run application
mvn spring-boot:run
```

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### Manual Testing
```bash
# Health check
curl http://localhost:8082/api/analysis/health

# Get indicators for BTCUSDT
curl http://localhost:8082/api/analysis/indicators/BTCUSDT

# Get RSI for BTCUSDT
curl http://localhost:8082/api/analysis/indicators/BTCUSDT/RSI

# Get analysis result
curl http://localhost:8082/api/analysis/result/BTCUSDT
```

## Monitoring

### Metrics
- Prometheus metrics at `/actuator/prometheus`
- Custom metrics for indicator calculations and processing times

### Buffer Monitoring
- Real-time buffer status via `/api/analysis/buffers`
- Buffer size and processing times

### Cache Monitoring
- Cache hit/miss rates
- Cache size and TTL statistics

## Performance Considerations

- **Batch Processing**: Processes data in batches for efficiency
- **Caching Strategy**: Multi-level caching with Redis
- **Async Processing**: Non-blocking Kafka consumers
- **Connection Pooling**: Optimized database connections

## Troubleshooting

### Common Issues

1. **High Memory Usage**
   - Check buffer sizes and processing frequency
   - Monitor Ta4j BarSeries cache

2. **Slow Indicator Calculations**
   - Verify sufficient historical data
   - Check Ta4j library performance

3. **Missing Indicators**
   - Ensure market data is flowing from Data Acquisition Service
   - Check Kafka consumer group status

### Debug Mode
```yaml
logging:
  level:
    com.cryptotrading: DEBUG
    org.ta4j: DEBUG
```
