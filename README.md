# Crypto Trading Agent System

A comprehensive real-time cryptocurrency trading system built with modern microservices architecture.

## Architecture Overview

- **Frontend**: Vue.js + TypeScript
- **API Gateway**: Spring Cloud Gateway
- **Backend Services**: Java 17 + Spring Boot microservices
- **Database**: PostgreSQL
- **Cache**: Redis
- **Message Streaming**: Apache Kafka
- **Containerization**: Docker & Docker Compose

## System Components

### Core Services
1. **Data Acquisition Service** - Real-time market data from Binance/Coinbase
2. **Data Analysis Service** - Technical indicators and price predictions
3. **Trading Strategy Service** - Rule-based trading strategies with backtesting
4. **Execution Service** - Order placement and tracking
5. **Risk Management Service** - Real-time risk monitoring and alerts
6. **API Gateway** - Centralized routing and security

### Infrastructure
- PostgreSQL database for persistent storage
- Redis for high-speed caching
- Kafka for real-time data streaming
- Docker containers for service deployment

## Quick Start

1. Clone the repository
2. Run `docker-compose up -d` to start infrastructure
3. Build and run individual services
4. Access the dashboard at `http://localhost:3000`

## Development Setup

See individual service README files for detailed setup instructions.

## License

MIT License
