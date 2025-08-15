#!/bin/bash

# Import Data Acquisition Service Dashboard to Grafana
echo "Importing Data Acquisition Service Dashboard..."

# Wait for Grafana to be ready
echo "Waiting for Grafana to be ready..."
until curl -s http://localhost:3002/api/health > /dev/null; do
    echo "Waiting for Grafana..."
    sleep 2
done

# Import the dashboard
curl -X POST \
  http://admin:admin@localhost:3002/api/dashboards/db \
  -H 'Content-Type: application/json' \
  -d @infrastructure/monitoring/grafana/provisioning/dashboards/data-acquisition-dashboard.json

echo "Dashboard import completed!"
echo "Access the dashboard at: http://localhost:3002"
echo "Login: admin / admin"
