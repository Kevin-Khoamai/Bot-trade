# Data Acquisition Service

The Data Acquisition Service is responsible for fetching real-time cryptocurrency market data from multiple exchanges (Binance and Coinbase), processing and validating the data, and distributing it through the system via Kafka streams and Redis cache.

## Features

- **Multi-Exchange Support**: Fetches data from Binance and Coinbase APIs
- **Real-time Streaming**: WebSocket connections for live market data
- **Data Validation**: Comprehensive validation of OHLCV data
- **Data Aggregation**: Combines data from multiple sources with weighted averages
- **Caching**: Redis-based caching for high-performance data access
- **Message Streaming**: Kafka integration for real-time data distribution
- **Circuit Breaker**: Resilience4j for handling API failures
- **Monitoring**: Prometheus metrics and health checks

## Architecture

```
┌─────────────────┐    ┌─────────────────┐
│   Binance API   │    │  Coinbase API   │
│   (REST/WS)     │    │   (REST/WS)     │
└─────────┬───────┘    └─────────┬───────┘
          │                      │
          ▼                      ▼
┌─────────────────────────────────────────┐
│        Data Acquisition Service         │
│  ┌─────────────┐  ┌─────────────────┐   │
│  │ REST Client │  │ WebSocket Client│   │
│  └─────────────┘  └─────────────────┘   │
│  ┌─────────────┐  ┌─────────────────┐   │
│  │ Validation  │  │   Aggregation   │   │
│  └─────────────┘  └─────────────────┘   │
└─────────┬───────────────────────────────┘
          │
          ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   PostgreSQL    │    │      Redis      │    │      Kafka      │
│   (Storage)     │    │    (Cache)      │    │   (Streaming)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## API Endpoints

### Health Check
- `GET /api/data/health` - Service health status

### Data Operations
- `POST /api/data/fetch` - Trigger manual data fetch for all symbols
- `POST /api/data/fetch/{symbol}` - Trigger data fetch for specific symbol
- `POST /api/data/fetch/current-prices` - Fetch current prices for all symbols

### Price Data
- `GET /api/data/price/{exchange}/{symbol}` - Get latest price for symbol
- `GET /api/data/prices/{symbol}` - Get historical prices with date range
- `GET /api/data/prices/{symbol}/recent?limit=100` - Get recent prices
- `GET /api/data/realtime/{exchange}/{symbol}` - Get cached real-time price
- `GET /api/data/ticker/{exchange}/{symbol}` - Get cached ticker data

### Metadata
- `GET /api/data/symbols/{exchange}` - Get all symbols for exchange
- `GET /api/data/exchanges/{symbol}` - Get all exchanges for symbol
- `GET /api/data/stats` - Get data statistics

## Configuration

### Application Properties
Key configuration properties in `application.yml`:

```yaml
# Exchange APIs
exchange:
  binance:
    base-url: https://api.binance.com
    websocket-url: wss://stream.binance.com:9443/ws
  coinbase:
    base-url: https://api.exchange.coinbase.com
    websocket-url: wss://ws-feed.exchange.coinbase.com

# Data Collection
data-collection:
  symbols: [BTCUSDT, ETHUSDT, ADAUSDT]
  intervals: [1m, 5m, 15m, 1h, 4h, 1d]
  fetch-interval: 300000  # 5 minutes
  cache-ttl: 30  # seconds

# Kafka Topics
kafka:
  topics:
    binance-trades: binance-trades
    coinbase-trades: coinbase-trades
    aggregated-data: aggregated-market-data
    price-updates: price-updates
```

## Data Flow

1. **REST API Fetching**: Scheduled task fetches historical data every 5 minutes
2. **WebSocket Streaming**: Real-time data streams from exchange WebSockets
3. **Data Validation**: All incoming data is validated for consistency and accuracy
4. **Caching**: Valid data is cached in Redis with TTL
5. **Database Storage**: Persistent storage in PostgreSQL
6. **Kafka Publishing**: Data is published to appropriate Kafka topics
7. **Aggregation**: Data from multiple exchanges is aggregated and validated

## Kafka Topics

- `binance-trades`: Raw data from Binance
- `coinbase-trades`: Raw data from Coinbase  
- `aggregated-market-data`: Aggregated data from multiple exchanges
- `price-updates`: Real-time price update notifications

## Redis Cache Keys

- `price:{exchange}:{symbol}` - Latest price data
- `ticker:{exchange}:{symbol}` - Ticker information
- `realtime:{exchange}:{symbol}` - Real-time price updates
- `stats:{exchange}:{symbol}` - 24hr statistics

## Building and Running

### Prerequisites
- Java 17+
- Maven 3.6+
- Docker and Docker Compose
- PostgreSQL, Redis, and Kafka (via Docker)

### Build
```bash
mvn clean package
```

### Run with Docker
```bash
docker build -t crypto-data-acquisition .
docker run -p 8081:8081 crypto-data-acquisition
```

### Run Locally
```bash
# Start infrastructure
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
curl http://localhost:8081/api/data/health

# Trigger data fetch
curl -X POST http://localhost:8081/api/data/fetch

# Get latest BTC price from Binance
curl http://localhost:8081/api/data/price/BINANCE/BTCUSDT
```

## Monitoring

### Metrics
- Prometheus metrics available at `/actuator/prometheus`
- Custom metrics for API calls, data processing, and errors

### Health Checks
- Spring Boot Actuator health endpoint
- Circuit breaker status
- Database connectivity
- Redis connectivity
- Kafka connectivity

### Logging
- Structured logging with correlation IDs
- Configurable log levels
- File and console output

## Error Handling

- **Circuit Breaker**: Protects against API failures
- **Retry Logic**: Automatic retries with exponential backoff
- **Fallback Methods**: Graceful degradation when services are unavailable
- **Data Validation**: Comprehensive validation prevents bad data propagation

## Performance Considerations

- **Connection Pooling**: Optimized database and Redis connections
- **Batch Processing**: Efficient batch operations for database writes
- **Caching Strategy**: Multi-level caching for optimal performance
- **Async Processing**: Non-blocking operations where possible

## Security

- **API Rate Limiting**: Respects exchange rate limits
- **Input Validation**: All inputs are validated and sanitized
- **Error Handling**: Sensitive information is not exposed in error messages

## Troubleshooting

### Common Issues

1. **WebSocket Connection Failures**
   - Check network connectivity
   - Verify WebSocket URLs
   - Review firewall settings

2. **Database Connection Issues**
   - Verify PostgreSQL is running
   - Check connection string and credentials
   - Review database logs

3. **Kafka Connection Problems**
   - Ensure Kafka is running and accessible
   - Verify topic creation
   - Check consumer group settings

### Debug Mode
Enable debug logging:
```yaml
logging:
  level:
    com.cryptotrading: DEBUG
```
