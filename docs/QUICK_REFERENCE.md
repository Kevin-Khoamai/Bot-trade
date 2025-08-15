# Crypto Trading System - Quick Reference

## 🚀 Essential Commands

### Service Management
```bash
# Start all infrastructure
docker-compose up -d

# Start Data Acquisition Service
cd services/data-acquisition
java -jar target/data-acquisition-service-1.0.0.jar

# Stop everything
pkill -f data-acquisition-service
docker-compose down
```

### Health Checks
```bash
# Data Acquisition Service
curl http://localhost:8081/actuator/health

# Kafka UI
curl http://localhost:8080/api/clusters

# All services status
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

### Real-time Data Monitoring
```bash
# Monitor live price updates
docker exec -it crypto-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic price-updates \
  --partition 0 \
  --offset latest \
  --property print.timestamp=true \
  --property print.key=true

# Monitor live trades
docker exec -it crypto-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic coinbase-trades \
  --partition 0 \
  --offset latest \
  --property print.timestamp=true \
  --property print.key=true

# View recent messages (safe command)
docker exec -it crypto-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic price-updates \
  --from-beginning \
  --max-messages 5 \
  --property print.timestamp=true
```

### Kafka Operations
```bash
# List all topics
docker exec -it crypto-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Check topic details
docker exec -it crypto-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic price-updates

# Check consumer groups
docker exec -it crypto-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group aggregation-group

# Get topic offsets
docker exec -it crypto-kafka kafka-run-class kafka.tools.GetOffsetShell \
  --bootstrap-server localhost:9092 \
  --topic price-updates
```

### Database Operations
```bash
# Connect to PostgreSQL
docker exec -it crypto-postgres psql -U crypto_user -d crypto_trading

# Check Redis
docker exec -it crypto-redis redis-cli ping
docker exec -it crypto-redis redis-cli info
```

## 🔧 Common Issues & Quick Fixes

### Issue: Data Acquisition Service Down
```bash
# Check if running
ps aux | grep data-acquisition

# Restart
cd services/data-acquisition
java -jar target/data-acquisition-service-1.0.0.jar
```

### Issue: Kafka UI Not Working
```bash
# Restart Kafka services
docker-compose restart kafka kafka-ui

# Check logs
docker logs crypto-kafka-ui
```

### Issue: No Real-time Data
```bash
# Check service health
curl http://localhost:8081/actuator/health

# Check latest Kafka messages
docker exec -it crypto-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic price-updates \
  --from-beginning \
  --max-messages 3
```

### Issue: Consumer Group Lag
```bash
# Reset consumer group
docker exec -it crypto-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group aggregation-group \
  --reset-offsets \
  --to-latest \
  --topic price-updates \
  --execute
```

## 📊 Monitoring URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| Kafka UI | http://localhost:8080 | None |
| Grafana | http://localhost:3002 | admin/admin |
| Prometheus | http://localhost:9090 | None |
| Data Service Health | http://localhost:8081/actuator/health | None |
| Data Service Metrics | http://localhost:8081/actuator/prometheus | None |

## 🎯 Key Topics

| Topic Name | Purpose | Key Data |
|------------|---------|----------|
| `price-updates` | Real-time price tickers | BTC, ETH, ADA prices with bid/ask |
| `coinbase-trades` | Trade executions | Trade size, price, side |
| `binance-trades` | Binance trade data | Trade executions from Binance |
| `aggregated-market-data` | Processed market data | Aggregated analytics |

## 🚨 Emergency Procedures

### Complete System Reset
```bash
# Stop everything
pkill -f data-acquisition-service
docker-compose down

# Clean restart
docker-compose up -d
sleep 30  # Wait for Kafka

# Start data service
cd services/data-acquisition
java -jar target/data-acquisition-service-1.0.0.jar
```

### Data Recovery
```bash
# Backup database
docker exec crypto-postgres pg_dump -U crypto_user crypto_trading > backup.sql

# Clear Redis cache
docker exec -it crypto-redis redis-cli flushall
```

## 📝 Log Locations

```bash
# Docker service logs
docker logs crypto-kafka
docker logs crypto-kafka-ui
docker logs crypto-postgres
docker logs crypto-redis
docker logs crypto-prometheus
docker logs crypto-grafana

# Data Acquisition Service
# Console output when running manually
```

## 🔍 Debugging Commands

```bash
# Check all containers
docker ps -a

# Check network connectivity
docker network ls
docker network inspect bot-trade_crypto-network

# Check resource usage
docker stats

# Check disk space
df -h
docker system df
```

## 📈 Performance Monitoring

```bash
# Kafka performance test
docker exec -it crypto-kafka kafka-run-class kafka.tools.ConsumerPerformance \
  --bootstrap-server localhost:9092 \
  --topic price-updates \
  --messages 100

# JVM metrics
curl http://localhost:8081/actuator/metrics/jvm.memory.used

# Database connections
docker exec -it crypto-postgres psql -U crypto_user -d crypto_trading \
  -c "SELECT count(*) FROM pg_stat_activity;"
```

## 🎛️ Configuration Files

| File | Purpose |
|------|---------|
| `docker-compose.yml` | Infrastructure setup |
| `services/data-acquisition/src/main/resources/application.yml` | Service config |
| `infrastructure/monitoring/prometheus/prometheus.yml` | Metrics config |
| `infrastructure/monitoring/grafana/provisioning/dashboards/` | Dashboards |

## 💡 Tips & Best Practices

1. **Always check service health first** before diving into complex debugging
2. **Use `--max-messages` flag** when testing Kafka consumers to avoid infinite output
3. **Monitor Grafana dashboards** for early warning signs
4. **Keep this reference handy** during operations
5. **Document any new issues** you encounter for future reference

---
**Quick Reference v1.0** | **Last Updated:** August 14, 2025
