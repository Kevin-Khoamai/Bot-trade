#!/bin/bash

# Crypto Trading Agent - System Integration Test Script
# Tests Modules 1 & 2 integration

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[TEST]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[FAIL]${NC} $1"
}

# Test counter
TESTS_PASSED=0
TESTS_FAILED=0
TOTAL_TESTS=0

# Function to run a test
run_test() {
    local test_name="$1"
    local test_command="$2"
    local expected_pattern="$3"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    print_status "Running: $test_name"
    
    if result=$(eval "$test_command" 2>&1); then
        if [[ -z "$expected_pattern" ]] || echo "$result" | grep -q "$expected_pattern"; then
            print_success "$test_name"
            TESTS_PASSED=$((TESTS_PASSED + 1))
            return 0
        else
            print_error "$test_name - Expected pattern not found: $expected_pattern"
            echo "Actual result: $result"
            TESTS_FAILED=$((TESTS_FAILED + 1))
            return 1
        fi
    else
        print_error "$test_name - Command failed"
        echo "Error: $result"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        return 1
    fi
}

# Function to wait for service
wait_for_service() {
    local service_name=$1
    local port=$2
    local max_attempts=30
    local attempt=1

    print_status "Waiting for $service_name to be ready on port $port..."
    
    while [ $attempt -le $max_attempts ]; do
        if nc -z localhost $port 2>/dev/null; then
            print_success "$service_name is ready!"
            return 0
        fi
        
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    print_error "$service_name failed to start within expected time"
    return 1
}

# Function to check if service is running
check_service() {
    local service_name=$1
    local container_name=$2
    
    if docker ps | grep -q "$container_name"; then
        print_success "$service_name container is running"
        return 0
    else
        print_error "$service_name container is not running"
        return 1
    fi
}

echo "🧪 Starting Crypto Trading Agent System Integration Tests..."
echo "Testing Modules 1 (Data Acquisition) & 2 (Analysis) Integration"
echo ""

# Phase 1: Infrastructure Health Checks
echo "=== Phase 1: Infrastructure Health Checks ==="

run_test "PostgreSQL Connection" \
    "docker exec crypto-postgres pg_isready -U crypto_user -d crypto_trading" \
    "accepting connections"

run_test "Redis Connection" \
    "docker exec crypto-redis redis-cli ping" \
    "PONG"

run_test "Kafka Broker Health" \
    "docker exec crypto-kafka kafka-broker-api-versions --bootstrap-server localhost:9092" \
    "kafka"

run_test "Kafka Topics Exist" \
    "docker exec crypto-kafka kafka-topics --list --bootstrap-server localhost:9092" \
    "binance-trades"

echo ""

# Phase 2: Service Health Checks
echo "=== Phase 2: Service Health Checks ==="

check_service "Data Acquisition Service" "crypto-data-acquisition"
check_service "Analysis Service" "crypto-analysis-service"

wait_for_service "Data Acquisition Service" 8081
wait_for_service "Analysis Service" 8082

run_test "Data Acquisition Health Check" \
    "curl -s http://localhost:8081/api/data/health" \
    "healthy"

run_test "Analysis Service Health Check" \
    "curl -s http://localhost:8082/api/analysis/health" \
    "healthy"

echo ""

# Phase 3: Data Acquisition Tests
echo "=== Phase 3: Data Acquisition Service Tests ==="

run_test "Data Service Statistics" \
    "curl -s http://localhost:8081/api/data/stats" \
    "totalRecords"

run_test "Available Symbols" \
    "curl -s http://localhost:8081/api/data/symbols/BINANCE" \
    "\\["

run_test "Trigger Manual Data Fetch" \
    "curl -s -X POST http://localhost:8081/api/data/fetch" \
    "triggered"

# Wait a bit for data to be processed
print_status "Waiting 30 seconds for data processing..."
sleep 30

run_test "Check Recent Price Data" \
    "curl -s 'http://localhost:8081/api/data/prices/BTCUSDT/recent?limit=5'" \
    "\\["

echo ""

# Phase 4: Analysis Service Tests
echo "=== Phase 4: Analysis Service Tests ==="

run_test "Analysis Service Statistics" \
    "curl -s http://localhost:8082/api/analysis/stats" \
    "totalIndicators"

run_test "Available Analysis Symbols" \
    "curl -s http://localhost:8082/api/analysis/symbols" \
    "\\["

run_test "Buffer Status Check" \
    "curl -s http://localhost:8082/api/analysis/buffers" \
    "\\{"

run_test "Cache Statistics" \
    "curl -s http://localhost:8082/api/analysis/cache/stats" \
    "indicatorCount"

echo ""

# Phase 5: Integration Tests
echo "=== Phase 5: Module Integration Tests ==="

# Trigger data fetch and wait for analysis
run_test "Trigger Data Fetch for BTCUSDT" \
    "curl -s -X POST http://localhost:8081/api/data/fetch/BTCUSDT" \
    "triggered"

print_status "Waiting 45 seconds for data processing and analysis..."
sleep 45

run_test "Check if Indicators Generated" \
    "curl -s http://localhost:8082/api/analysis/indicators/BTCUSDT" \
    "\\["

run_test "Check RSI Indicator" \
    "curl -s http://localhost:8082/api/analysis/indicators/BTCUSDT/RSI" \
    "indicatorType"

run_test "Check Analysis Result" \
    "curl -s http://localhost:8082/api/analysis/result/BTCUSDT" \
    "symbol"

run_test "Check Trading Signal" \
    "curl -s http://localhost:8082/api/analysis/signal/BTCUSDT" \
    "signal"

echo ""

# Phase 6: Kafka Integration Tests
echo "=== Phase 6: Kafka Integration Tests ==="

run_test "Check Kafka Consumer Groups" \
    "docker exec crypto-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list" \
    "analysis-group"

run_test "Check Topic Message Count" \
    "docker exec crypto-kafka kafka-run-class kafka.tools.GetOffsetShell --broker-list localhost:9092 --topic binance-trades" \
    ":"

echo ""

# Phase 7: Performance Tests
echo "=== Phase 7: Performance Tests ==="

run_test "Data Service Response Time" \
    "time curl -s http://localhost:8081/api/data/health > /dev/null" \
    ""

run_test "Analysis Service Response Time" \
    "time curl -s http://localhost:8082/api/analysis/health > /dev/null" \
    ""

run_test "Database Connection Pool" \
    "curl -s http://localhost:8081/actuator/metrics/hikaricp.connections.active" \
    "hikaricp"

run_test "Redis Connection Pool" \
    "curl -s http://localhost:8082/actuator/metrics" \
    "redis"

echo ""

# Phase 8: Data Flow Verification
echo "=== Phase 8: Data Flow Verification ==="

# Force process analysis buffer
run_test "Force Process Analysis Buffer" \
    "curl -s -X POST http://localhost:8082/api/analysis/buffers/BTCUSDT/process" \
    "triggered"

sleep 10

run_test "Verify Indicator Types Available" \
    "curl -s http://localhost:8082/api/analysis/indicators/BTCUSDT/types" \
    "RSI"

run_test "Check Historical Indicators" \
    "curl -s 'http://localhost:8082/api/analysis/indicators/BTCUSDT/RSI/recent?limit=5'" \
    "\\["

echo ""

# Phase 9: Error Handling Tests
echo "=== Phase 9: Error Handling Tests ==="

run_test "Invalid Symbol Request" \
    "curl -s -w '%{http_code}' http://localhost:8081/api/data/price/BINANCE/INVALID" \
    "404"

run_test "Invalid Indicator Request" \
    "curl -s -w '%{http_code}' http://localhost:8082/api/analysis/indicators/INVALID/RSI" \
    "404"

echo ""

# Final Results
echo "=== Test Results Summary ==="
echo "Total Tests: $TOTAL_TESTS"
echo "Passed: $TESTS_PASSED"
echo "Failed: $TESTS_FAILED"

if [ $TESTS_FAILED -eq 0 ]; then
    print_success "🎉 All tests passed! Modules 1 & 2 are working correctly together."
    echo ""
    echo "✅ Data Acquisition Service is fetching market data"
    echo "✅ Analysis Service is consuming data and generating indicators"
    echo "✅ Kafka integration is working properly"
    echo "✅ Redis caching is functional"
    echo "✅ PostgreSQL storage is working"
    echo "✅ REST APIs are responding correctly"
    echo ""
    echo "🚀 System is ready for Module 3 (Trading Strategy Service)!"
    exit 0
else
    print_error "❌ $TESTS_FAILED test(s) failed. Please check the issues above."
    echo ""
    echo "🔧 Troubleshooting tips:"
    echo "  • Check service logs: docker logs crypto-data-acquisition"
    echo "  • Check service logs: docker logs crypto-analysis-service"
    echo "  • Verify all containers are running: docker ps"
    echo "  • Check Kafka topics: docker exec crypto-kafka kafka-topics --list --bootstrap-server localhost:9092"
    exit 1
fi
