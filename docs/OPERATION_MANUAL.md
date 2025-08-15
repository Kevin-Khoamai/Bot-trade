# Crypto Trading System - Operation Manual

## Table of Contents
1. [System Overview](#system-overview)
2. [Common Issues & Troubleshooting](#common-issues--troubleshooting)
3. [Service Management](#service-management)
4. [Monitoring & Debugging](#monitoring--debugging)
5. [Kafka Operations](#kafka-operations)
6. [Emergency Procedures](#emergency-procedures)

## System Overview

### Architecture Components
- **Data Acquisition Service** (Port 8081) - Collects crypto data from exchanges
- **Kafka** (Port 9092) - Message broker for real-time data streaming
- **Kafka UI** (Port 8080) - Web interface for Kafka monitoring
- **PostgreSQL** (Port 5432) - Primary database
- **Redis** (Port 6379) - Caching layer
- **Prometheus** (Port 9090) - Metrics collection
- **Grafana** (Port 3002) - Monitoring dashboards

### Data Flow
```
Coinbase/Binance → Data Acquisition Service → Kafka → Database/Cache
                                          ↓
                                    Monitoring Tools
```

## Common Issues & Troubleshooting

### Issue 1: Data Acquisition Service Stops Working

**Symptoms:**
- No new data in Kafka topics
- Service health check fails: `curl http://localhost:8081/actuator/health`
- Timestamps in Kafka UI stop updating

**Root Cause:**
Service crashes or loses connection to external APIs/Kafka

**Troubleshooting Steps:**

1. **Check Service Status:**
```bash
curl -s http://localhost:8081/actuator/health
```

2. **Check Running Processes:**
```bash
ps aux | grep data-acquisition
```

3. **Restart Service:**
```bash
cd services/data-acquisition
java -jar target/data-acquisition-service-1.0.0.jar
```

4. **Monitor Startup Logs:**
Look for:
- ✅ "Coinbase WebSocket connection established"
- ✅ "Kafka version: 3.6.0"
- ✅ "Started DataAcquisitionApplication"

**Prevention:**
- Monitor service health endpoint regularly
- Set up automatic restart mechanisms
- Monitor log files for early warning signs

### Issue 2: Kafka UI Shows "Service Not Running"

**Symptoms:**
- Kafka UI displays connection errors
- Cannot see topics or consumer groups
- API calls to Kafka UI return 404/500 errors

**Root Cause:**
Kafka network configuration mismatch between internal and external listeners

**Troubleshooting Steps:**

1. **Check Kafka Container Status:**
```bash
docker ps | grep kafka
```

2. **Verify Kafka Configuration:**
```bash
docker logs crypto-kafka --tail 20
```

3. **Test Kafka Connectivity:**
```bash
docker exec -it crypto-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

4. **Fix Network Configuration:**
Update `docker-compose.yml`:
```yaml
kafka:
  environment:
    KAFKA_LISTENERS: INTERNAL://0.0.0.0:29092,EXTERNAL://0.0.0.0:9092
    KAFKA_ADVERTISED_LISTENERS: INTERNAL://kafka:29092,EXTERNAL://localhost:9092
    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT
    KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL

kafka-ui:
  environment:
    KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092  # Use internal listener
```

5. **Restart Kafka Services:**
```bash
docker-compose down kafka kafka-ui
docker-compose up -d kafka kafka-ui
```

**Prevention:**
- Always use internal network names for container-to-container communication
- Test Kafka UI connectivity after any network changes

### Issue 3: Real-time Data Monitoring Commands Fail

**Symptoms:**
- `docker exec` commands return errors
- No output from Kafka console consumer
- "The partition is required when offset is specified" errors

**Root Cause:**
Incorrect Kafka console consumer command syntax or missing parameters

**Working Commands:**

1. **Basic Data Viewing:**
```bash
docker exec -it crypto-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic price-updates \
  --from-beginning \
  --max-messages 5
```

2. **Real-time Monitoring:**
```bash
docker exec -it crypto-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic price-updates \
  --partition 0 \
  --offset latest \
  --property print.timestamp=true \
  --property print.key=true
```

3. **Recent Messages:**
```bash
docker exec -it crypto-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic price-updates \
  --from-beginning \
  --property print.timestamp=true \
  --max-messages 10
```

**Common Mistakes:**
- ❌ Using `--offset latest` without `--partition`
- ❌ Wrong bootstrap server address
- ❌ Missing topic name
- ❌ Incorrect property syntax

### Issue 4: Grafana Dashboard Import Fails

**Symptoms:**
- Dashboard not visible in Grafana
- Import script returns errors
- Metrics not displaying correctly

**Troubleshooting Steps:**

1. **Check Grafana Status:**
```bash
curl -s http://localhost:3002/api/health
```

2. **Verify Prometheus Connection:**
```bash
curl -s http://localhost:9090/api/v1/query?query=up
```

3. **Re-import Dashboard:**
```bash
./scripts/import-dashboard.sh
```

4. **Manual Import:**
- Open http://localhost:3002
- Login: admin/admin
- Go to Dashboards → Import
- Upload `infrastructure/monitoring/grafana/provisioning/dashboards/data-acquisition-dashboard.json`

## Service Management

### Starting All Services
```bash
docker-compose up -d
cd services/data-acquisition
java -jar target/data-acquisition-service-1.0.0.jar
```

### Stopping Services
```bash
# Stop Data Acquisition Service
pkill -f data-acquisition-service

# Stop Docker services
docker-compose down
```

### Health Checks
```bash
# Data Acquisition Service
curl http://localhost:8081/actuator/health

# Kafka UI
curl http://localhost:8080/api/clusters

# Grafana
curl http://localhost:3002/api/health

# Prometheus
curl http://localhost:9090/-/healthy
```

## Monitoring & Debugging

### Key Metrics to Monitor

1. **Service Health:**
   - Data Acquisition Service uptime
   - Kafka broker status
   - Database connections

2. **Data Flow:**
   - Kafka message throughput
   - Topic lag
   - Consumer group status

3. **Performance:**
   - Response times
   - Memory usage
   - Error rates

### Log Locations

1. **Data Acquisition Service:**
   - Console output when running manually
   - Application logs in service directory

2. **Docker Services:**
```bash
docker logs crypto-kafka
docker logs crypto-kafka-ui
docker logs crypto-postgres
docker logs crypto-redis
```

### Debugging Commands

1. **Check Kafka Topics:**
```bash
docker exec -it crypto-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

2. **Check Consumer Groups:**
```bash
docker exec -it crypto-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group aggregation-group
```

3. **Check Topic Offsets:**
```bash
docker exec -it crypto-kafka kafka-run-class kafka.tools.GetOffsetShell \
  --bootstrap-server localhost:9092 \
  --topic price-updates
```

## Kafka Operations

### Real-time Data Monitoring

1. **Monitor Price Updates:**
```bash
docker exec -it crypto-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic price-updates \
  --partition 0 \
  --offset latest \
  --property print.timestamp=true \
  --property print.key=true
```

2. **Monitor Trade Data:**
```bash
docker exec -it crypto-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic coinbase-trades \
  --partition 0 \
  --offset latest \
  --property print.timestamp=true \
  --property print.key=true
```

3. **View Recent Messages:**
```bash
docker exec -it crypto-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic price-updates \
  --from-beginning \
  --max-messages 10 \
  --property print.timestamp=true
```

### Topic Management

1. **List Topics:**
```bash
docker exec -it crypto-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

2. **Describe Topic:**
```bash
docker exec -it crypto-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic price-updates
```

3. **Create Topic (if needed):**
```bash
docker exec -it crypto-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create \
  --topic new-topic \
  --partitions 1 \
  --replication-factor 1
```

## Emergency Procedures

### Complete System Restart

1. **Stop All Services:**
```bash
pkill -f data-acquisition-service
docker-compose down
```

2. **Clean Up (if needed):**
```bash
docker-compose down -v  # Removes volumes
docker system prune     # Clean up unused containers
```

3. **Restart Infrastructure:**
```bash
docker-compose up -d
```

4. **Wait for Services to Initialize:**
```bash
# Wait 30 seconds for Kafka to be ready
sleep 30
```

5. **Start Data Acquisition Service:**
```bash
cd services/data-acquisition
java -jar target/data-acquisition-service-1.0.0.jar
```

6. **Verify System Health:**
```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8080/api/clusters
```

### Data Recovery

If data is lost or corrupted:

1. **Check Database Status:**
```bash
docker exec -it crypto-postgres psql -U crypto_user -d crypto_trading -c "\dt"
```

2. **Check Redis Cache:**
```bash
docker exec -it crypto-redis redis-cli ping
```

3. **Restart Data Collection:**
The system will automatically start collecting new data once services are running.

### Contact Information

For critical issues:
- Check system logs first
- Verify all services are running
- Follow troubleshooting steps in this manual
- Document any new issues for future reference

## Advanced Troubleshooting Scenarios

### Scenario 1: Data Stops Flowing After System Restart

**Problem:** After restarting Docker services, Kafka UI shows old data timestamps.

**Solution:**
1. Check if Data Acquisition Service is still running:
```bash
ps aux | grep data-acquisition
```

2. If not running, restart it:
```bash
cd services/data-acquisition
java -jar target/data-acquisition-service-1.0.0.jar
```

3. Monitor logs for successful connections:
- Look for "Coinbase WebSocket connection established"
- Look for Kafka producer messages with new offsets

### Scenario 2: WebSocket Connection Failures

**Problem:** Service logs show "Failed to connect" for WebSocket connections.

**Symptoms:**
```
Failed to connect
java.util.concurrent.CompletionException: jakarta.websocket.DeploymentException:
The HTTP response from the server [404] did not permit the HTTP upgrade to WebSocket
```

**Solutions:**
1. **For Binance WebSocket (404 errors):**
   - This is expected for Binance in some regions
   - Coinbase connection should still work
   - System will continue with Coinbase data only

2. **For Coinbase WebSocket failures:**
   - Check internet connectivity
   - Verify firewall settings
   - Restart the service

### Scenario 3: Kafka Consumer Group Issues

**Problem:** Consumer groups show lag or are not processing messages.

**Diagnosis:**
```bash
docker exec -it crypto-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group aggregation-group
```

**Solutions:**
1. **Reset Consumer Group (if needed):**
```bash
docker exec -it crypto-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group aggregation-group \
  --reset-offsets \
  --to-latest \
  --topic price-updates \
  --execute
```

2. **Restart Data Acquisition Service** to rejoin consumer group

### Scenario 4: Database Connection Issues

**Problem:** Service cannot connect to PostgreSQL.

**Symptoms:**
- Connection timeout errors in logs
- Health check fails
- No data being persisted

**Solutions:**
1. **Check PostgreSQL Status:**
```bash
docker exec -it crypto-postgres pg_isready -U crypto_user
```

2. **Verify Database Exists:**
```bash
docker exec -it crypto-postgres psql -U crypto_user -l
```

3. **Check Connection String:**
Verify in `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/crypto_trading
    username: crypto_user
    password: crypto_password
```

### Scenario 5: Redis Cache Issues

**Problem:** Caching not working, performance degraded.

**Diagnosis:**
```bash
docker exec -it crypto-redis redis-cli ping
docker exec -it crypto-redis redis-cli info memory
```

**Solutions:**
1. **Clear Cache (if corrupted):**
```bash
docker exec -it crypto-redis redis-cli flushall
```

2. **Restart Redis:**
```bash
docker-compose restart redis
```

## Performance Optimization

### Monitoring Performance

1. **Check Kafka Throughput:**
```bash
docker exec -it crypto-kafka kafka-run-class kafka.tools.ConsumerPerformance \
  --bootstrap-server localhost:9092 \
  --topic price-updates \
  --messages 1000
```

2. **Monitor JVM Memory:**
```bash
curl http://localhost:8081/actuator/metrics/jvm.memory.used
```

3. **Check Database Performance:**
```bash
docker exec -it crypto-postgres psql -U crypto_user -d crypto_trading \
  -c "SELECT * FROM pg_stat_activity;"
```

### Optimization Tips

1. **Increase JVM Memory (if needed):**
```bash
java -Xmx2g -jar target/data-acquisition-service-1.0.0.jar
```

2. **Kafka Configuration Tuning:**
   - Increase `batch.size` for higher throughput
   - Adjust `linger.ms` for batching
   - Monitor consumer lag

3. **Database Optimization:**
   - Add indexes on frequently queried columns
   - Monitor connection pool usage
   - Consider read replicas for heavy queries

## Backup and Recovery

### Database Backup

1. **Create Backup:**
```bash
docker exec crypto-postgres pg_dump -U crypto_user crypto_trading > backup.sql
```

2. **Restore Backup:**
```bash
docker exec -i crypto-postgres psql -U crypto_user crypto_trading < backup.sql
```

### Configuration Backup

Important files to backup:
- `docker-compose.yml`
- `services/data-acquisition/src/main/resources/application.yml`
- `infrastructure/monitoring/grafana/provisioning/dashboards/`
- `infrastructure/monitoring/prometheus/prometheus.yml`

## Security Considerations

### Network Security
- All services run on localhost by default
- Consider using Docker networks for isolation
- Implement proper firewall rules for production

### Authentication
- Change default Grafana credentials (admin/admin)
- Use environment variables for sensitive configuration
- Implement proper database user permissions

### Data Protection
- Enable SSL/TLS for database connections in production
- Encrypt sensitive configuration values
- Regular security updates for all components

## Maintenance Schedule

### Daily Tasks
- Check service health endpoints
- Monitor Grafana dashboards
- Verify data flow in Kafka UI

### Weekly Tasks
- Review system logs for errors
- Check disk space usage
- Monitor database performance

### Monthly Tasks
- Update system dependencies
- Review and optimize configurations
- Backup critical data and configurations

## Troubleshooting Checklist

When issues occur, follow this checklist:

1. **✅ Check Service Status**
   - [ ] Data Acquisition Service health
   - [ ] Docker containers running
   - [ ] Network connectivity

2. **✅ Verify Data Flow**
   - [ ] Kafka topics receiving messages
   - [ ] Consumer groups processing
   - [ ] Database updates

3. **✅ Review Logs**
   - [ ] Application logs for errors
   - [ ] Docker container logs
   - [ ] System resource usage

4. **✅ Test Connectivity**
   - [ ] WebSocket connections
   - [ ] Database connections
   - [ ] Kafka connectivity

5. **✅ Restart if Necessary**
   - [ ] Individual services
   - [ ] Docker containers
   - [ ] Complete system restart

---

**Last Updated:** August 14, 2025
**Version:** 1.0
**Authors:** System Operations Team
