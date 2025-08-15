# 📚 Bot-Trade Documentation

Welcome to the comprehensive documentation for the Bot-Trade cryptocurrency trading platform.

## 📖 Available Documentation

### 🔄 [Data Acquisition Architecture](./DATA_ACQUISITION_ARCHITECTURE.md)
**Complete technical documentation for the data collection system**
- Detailed data flow from Binance and Coinbase APIs
- WebSocket streaming implementation
- Kafka message processing
- PostgreSQL database schema
- Redis caching strategy
- Source code analysis with function-by-function explanations

### 🛠️ [Operation Manual](./OPERATION_MANUAL.md)
**Step-by-step guide for running the platform**
- Service startup procedures
- Configuration management
- Monitoring and maintenance

### ⚡ [Quick Reference](./QUICK_REFERENCE.md)
**Fast access to common commands and configurations**
- Docker commands
- API endpoints
- Database queries

### 🔧 [Troubleshooting Flowchart](./TROUBLESHOOTING_FLOWCHART.md)
**Diagnostic guide for common issues**
- Service connectivity problems
- Data flow issues
- Performance optimization

## 🏗️ System Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Binance API   │    │  Coinbase API   │    │ Coinbase WebSocket│
│   (REST)        │    │   (REST)        │    │   (Real-time)   │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
                    ┌─────────────▼─────────────┐
                    │  Data Acquisition Service │
                    │     (Spring Boot)         │
                    └─────────────┬─────────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          │                       │                       │
          ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   PostgreSQL    │    │     Redis       │    │     Kafka       │
│   Database      │    │     Cache       │    │   Streaming     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
          │                       │                       │
          └───────────────────────┼───────────────────────┘
                                  │
                    ┌─────────────▼─────────────┐
                    │      API Gateway          │
                    │     (Port 8082)           │
                    └─────────────┬─────────────┘
                                  │
                    ┌─────────────▼─────────────┐
                    │    Vue.js Frontend        │
                    │     (Port 3000)           │
                    └───────────────────────────┘
```

## 🚀 Quick Start

1. **Start Infrastructure**
   ```bash
   cd services/data-acquisition
   docker-compose up -d
   ```

2. **Start Data Acquisition Service**
   ```bash
   mvn spring-boot:run
   ```

3. **Start API Gateway**
   ```bash
   cd services/api-gateway
   mvn spring-boot:run
   ```

4. **Start Frontend**
   ```bash
   cd vue-dashboard
   npm run dev
   ```

## 📊 Key Features

- **Real-time Data Collection** - WebSocket streaming from Coinbase
- **Multi-Exchange Support** - Binance and Coinbase integration
- **High-Performance Caching** - Redis for sub-second data access
- **Scalable Messaging** - Kafka for real-time data distribution
- **Robust Storage** - PostgreSQL with optimized indexing
- **Professional UI** - Vue.js dashboard with TradingView charts

## 🔗 Related Links

- [Main README](../README.md) - Project overview and setup
- [Data Acquisition Service](../services/data-acquisition/README.md) - Service-specific documentation
- [API Gateway](../services/api-gateway/README.md) - Gateway configuration
- [Vue Dashboard](../vue-dashboard/README.md) - Frontend documentation

## 📞 Support

For technical questions or issues:
1. Check the [Troubleshooting Guide](./TROUBLESHOOTING_FLOWCHART.md)
2. Review the [Operation Manual](./OPERATION_MANUAL.md)
3. Examine the [Data Architecture](./DATA_ACQUISITION_ARCHITECTURE.md) for detailed technical information

---

*Last updated: January 2024*
