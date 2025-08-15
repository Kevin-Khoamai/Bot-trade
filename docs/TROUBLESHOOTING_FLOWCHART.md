# Crypto Trading System - Troubleshooting Flowchart

## 🔄 Main Troubleshooting Flow

```
🚨 ISSUE DETECTED
        ↓
📊 Check System Status
        ↓
┌─────────────────────┐
│ All Services Up?    │
│ docker ps           │
└─────────────────────┘
        ↓ NO                    ↓ YES
┌─────────────────────┐    ┌─────────────────────┐
│ Start Missing       │    │ Check Data Flow     │
│ Services            │    │                     │
│ docker-compose up   │    │ curl health checks  │
└─────────────────────┘    └─────────────────────┘
        ↓                         ↓ NO DATA
┌─────────────────────┐    ┌─────────────────────┐
│ Wait 30 seconds     │    │ Data Acquisition    │
│ for initialization  │    │ Service Running?    │
└─────────────────────┘    └─────────────────────┘
        ↓                         ↓ NO
┌─────────────────────┐    ┌─────────────────────┐
│ Test Again          │    │ Restart Data        │
└─────────────────────┘    │ Acquisition Service │
                           └─────────────────────┘
                                   ↓
                           ┌─────────────────────┐
                           │ Monitor Logs for    │
                           │ Successful Startup  │
                           └─────────────────────┘
```

## 🎯 Specific Issue Flows

### Data Flow Issues

```
❌ NO NEW DATA IN KAFKA
        ↓
🔍 Check Data Acquisition Service
        ↓
curl http://localhost:8081/actuator/health
        ↓
┌─────────────────────┐
│ Service Healthy?    │
└─────────────────────┘
    ↓ NO        ↓ YES
┌─────────┐  ┌─────────────────────┐
│ Restart │  │ Check WebSocket     │
│ Service │  │ Connections in Logs │
└─────────┘  └─────────────────────┘
                      ↓
              ┌─────────────────────┐
              │ Coinbase Connected? │
              └─────────────────────┘
                  ↓ NO        ↓ YES
              ┌─────────┐  ┌─────────────────────┐
              │ Network │  │ Check Kafka         │
              │ Issue   │  │ Producer Logs       │
              └─────────┘  └─────────────────────┘
                                   ↓
                           ┌─────────────────────┐
                           │ Messages Being Sent │
                           │ to Kafka Topics?    │
                           └─────────────────────┘
                               ↓ NO        ↓ YES
                           ┌─────────┐  ┌─────────┐
                           │ Kafka   │  │ Check   │
                           │ Issue   │  │ Offsets │
                           └─────────┘  └─────────┘
```

### Kafka UI Issues

```
🖥️ KAFKA UI NOT WORKING
        ↓
🌐 Open http://localhost:8080
        ↓
┌─────────────────────┐
│ Page Loads?         │
└─────────────────────┘
    ↓ NO        ↓ YES
┌─────────┐  ┌─────────────────────┐
│ Check   │  │ Shows "Service      │
│ Docker  │  │ Not Running"?       │
│ Status  │  └─────────────────────┘
└─────────┘      ↓ YES        ↓ NO
                ┌─────────┐  ┌─────────┐
                │ Network │  │ Working │
                │ Config  │  │ Fine    │
                │ Issue   │  └─────────┘
                └─────────┘
                    ↓
            ┌─────────────────────┐
            │ Fix docker-compose  │
            │ Kafka listeners     │
            └─────────────────────┘
                    ↓
            ┌─────────────────────┐
            │ Restart kafka &     │
            │ kafka-ui services   │
            └─────────────────────┘
```

### Monitoring Commands Issues

```
💻 KAFKA CONSOLE COMMANDS FAIL
        ↓
🔍 Check Error Message
        ↓
┌─────────────────────────────┐
│ "partition is required"?    │
└─────────────────────────────┘
    ↓ YES                ↓ NO
┌─────────────────┐  ┌─────────────────────┐
│ Add --partition │  │ "connection refused"│
│ 0 parameter     │  └─────────────────────┘
└─────────────────┘      ↓ YES        ↓ NO
                    ┌─────────┐  ┌─────────────────┐
                    │ Kafka   │  │ Check command   │
                    │ Down    │  │ syntax          │
                    └─────────┘  └─────────────────┘
                        ↓
                ┌─────────────────┐
                │ Start Kafka:    │
                │ docker-compose  │
                │ up -d kafka     │
                └─────────────────┘
```

## 🚨 Emergency Decision Tree

```
🆘 CRITICAL SYSTEM FAILURE
        ↓
⏰ How Critical is the Issue?
        ↓
┌─────────────────────────────┐
│ Data Loss Risk?             │
└─────────────────────────────┘
    ↓ YES                ↓ NO
┌─────────────────┐  ┌─────────────────────┐
│ 1. Stop all     │  │ 1. Try service      │
│    services     │  │    restart first    │
│ 2. Backup DB    │  │ 2. Check logs       │
│ 3. Investigate  │  │ 3. Gradual restart  │
└─────────────────┘  └─────────────────────┘
        ↓                        ↓
┌─────────────────┐  ┌─────────────────────┐
│ Full system     │  │ Monitor recovery    │
│ restore if      │  │ and document issue  │
│ necessary       │  └─────────────────────┘
└─────────────────┘
```

## 📋 Quick Diagnostic Checklist

### Level 1: Basic Checks (30 seconds)
```bash
# 1. Container status
docker ps | grep -E "(kafka|postgres|redis)"

# 2. Service health
curl -s http://localhost:8081/actuator/health

# 3. Kafka UI access
curl -s http://localhost:8080/api/clusters
```

### Level 2: Data Flow Checks (2 minutes)
```bash
# 1. Recent Kafka messages
docker exec -it crypto-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic price-updates \
  --from-beginning \
  --max-messages 3

# 2. Topic offsets
docker exec -it crypto-kafka kafka-run-class kafka.tools.GetOffsetShell \
  --bootstrap-server localhost:9092 \
  --topic price-updates

# 3. Consumer group status
docker exec -it crypto-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group aggregation-group
```

### Level 3: Deep Diagnostics (5 minutes)
```bash
# 1. Service logs
docker logs crypto-kafka --tail 20
docker logs crypto-kafka-ui --tail 20

# 2. Database connectivity
docker exec -it crypto-postgres pg_isready -U crypto_user

# 3. Redis connectivity
docker exec -it crypto-redis redis-cli ping

# 4. Network inspection
docker network inspect bot-trade_crypto-network
```

## 🎯 Resolution Priorities

### Priority 1: Critical (Fix Immediately)
- Data Acquisition Service down
- Database connection lost
- Kafka broker failure

### Priority 2: High (Fix within 1 hour)
- Kafka UI not accessible
- Consumer group lag
- WebSocket connection issues

### Priority 3: Medium (Fix within 4 hours)
- Monitoring dashboard issues
- Performance degradation
- Cache connectivity problems

### Priority 4: Low (Fix within 24 hours)
- Documentation updates
- Configuration optimization
- Non-critical log warnings

## 🔧 Common Resolution Patterns

### Pattern 1: Service Restart
```bash
# For most service-level issues
pkill -f data-acquisition-service
cd services/data-acquisition
java -jar target/data-acquisition-service-1.0.0.jar
```

### Pattern 2: Infrastructure Restart
```bash
# For Docker/Kafka issues
docker-compose restart kafka kafka-ui
# Wait 30 seconds
# Restart data service
```

### Pattern 3: Full System Reset
```bash
# For complex multi-service issues
pkill -f data-acquisition-service
docker-compose down
docker-compose up -d
sleep 30
cd services/data-acquisition
java -jar target/data-acquisition-service-1.0.0.jar
```

### Pattern 4: Configuration Fix
```bash
# For network/config issues
# 1. Edit docker-compose.yml
# 2. docker-compose down
# 3. docker-compose up -d
# 4. Restart dependent services
```

---

**Use this flowchart to systematically diagnose and resolve issues.**
**Always start with the simplest checks first.**
**Document any new patterns you discover.**

**Troubleshooting Guide v1.0** | **Last Updated:** August 14, 2025
