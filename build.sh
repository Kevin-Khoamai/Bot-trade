#!/bin/bash

# Crypto Trading Agent - Build Script
# This script builds and starts the entire system

set -e

echo "🚀 Building Crypto Trading Agent System..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if Docker Compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "Docker Compose is not installed. Please install Docker Compose and try again."
    exit 1
fi

# Function to wait for service to be ready
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

# Start infrastructure services
print_status "Starting infrastructure services..."
docker-compose up -d postgres redis zookeeper kafka

# Wait for infrastructure to be ready
wait_for_service "PostgreSQL" 5433
wait_for_service "Redis" 6380
wait_for_service "Kafka" 9092

# Start monitoring services
print_status "Starting monitoring services..."
docker-compose up -d kafka-ui prometheus grafana

# Build Data Acquisition Service
print_status "Building Data Acquisition Service..."
cd services/data-acquisition

if [ -f "pom.xml" ]; then
    print_status "Building with Maven..."
    mvn clean package -DskipTests

    if [ $? -eq 0 ]; then
        print_success "Data Acquisition Service built successfully"
    else
        print_error "Failed to build Data Acquisition Service"
        exit 1
    fi
else
    print_error "pom.xml not found in data-acquisition service"
    exit 1
fi

# Build Docker image for Data Acquisition Service
print_status "Building Docker image for Data Acquisition Service..."
docker build -t crypto-data-acquisition:latest .

if [ $? -eq 0 ]; then
    print_success "Data Acquisition Service Docker image built successfully"
else
    print_error "Failed to build Data Acquisition Service Docker image"
    exit 1
fi

cd ../..

# Build Analysis Service
print_status "Building Analysis Service..."
cd services/analysis-service

if [ -f "pom.xml" ]; then
    print_status "Building with Maven..."
    mvn clean package -DskipTests

    if [ $? -eq 0 ]; then
        print_success "Analysis Service built successfully"
    else
        print_error "Failed to build Analysis Service"
        exit 1
    fi
else
    print_error "pom.xml not found in analysis service"
    exit 1
fi

# Build Docker image for Analysis Service
print_status "Building Docker image for Analysis Service..."
docker build -t crypto-analysis-service:latest .

if [ $? -eq 0 ]; then
    print_success "Analysis Service Docker image built successfully"
else
    print_error "Failed to build Analysis Service Docker image"
    exit 1
fi

cd ../..

# Create Kafka topics
print_status "Creating Kafka topics..."
docker exec crypto-kafka kafka-topics --create --topic binance-trades --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 --if-not-exists
docker exec crypto-kafka kafka-topics --create --topic coinbase-trades --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 --if-not-exists
docker exec crypto-kafka kafka-topics --create --topic aggregated-market-data --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 --if-not-exists
docker exec crypto-kafka kafka-topics --create --topic price-updates --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 --if-not-exists

print_success "Kafka topics created successfully"

# Start Data Acquisition Service
print_status "Starting Data Acquisition Service..."
docker run -d \
    --name crypto-data-acquisition \
    --network crypto-network \
    -p 8081:8081 \
    -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/crypto_trading \
    -e SPRING_DATA_REDIS_HOST=redis \
    -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
    crypto-data-acquisition:latest

# Wait for Data Acquisition Service to be ready
wait_for_service "Data Acquisition Service" 8081

# Start Analysis Service
print_status "Starting Analysis Service..."
docker run -d \
    --name crypto-analysis-service \
    --network crypto-network \
    -p 8082:8082 \
    -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/crypto_trading \
    -e SPRING_DATA_REDIS_HOST=redis \
    -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
    crypto-analysis-service:latest

# Wait for Analysis Service to be ready
wait_for_service "Analysis Service" 8082

print_success "🎉 Crypto Trading Agent System is now running!"

echo ""
echo "📊 Service URLs:"
echo "  • Data Acquisition API: http://localhost:8081/api/data"
echo "  • Analysis API: http://localhost:8082/api/analysis"
echo "  • Kafka UI: http://localhost:8080"
echo "  • Prometheus: http://localhost:9090"
echo "  • Grafana: http://localhost:3002 (admin/admin)"
echo ""

echo "🔧 Useful commands:"
echo "  • View data logs: docker logs crypto-data-acquisition"
echo "  • View analysis logs: docker logs crypto-analysis-service"
echo "  • Stop services: docker-compose down"
echo "  • Restart data service: docker restart crypto-data-acquisition"
echo "  • Restart analysis service: docker restart crypto-analysis-service"
echo ""

print_status "System startup complete! Check the service URLs above to verify everything is working."
