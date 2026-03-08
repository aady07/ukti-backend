#!/bin/bash

# Ukti Education Backend - Deploy to EC2 on port 8082
# Usage: Run from EC2

cd /home/ec2-user/Ukti/ukti-backend || exit

# Checkout main branch and pull latest changes
git checkout main
git pull origin main

# Build the project
mvn clean package -DskipTests

# Find the newly built JAR
JAR_FILE=$(ls target/*.jar 2>/dev/null | head -n 1)

if [ -z "$JAR_FILE" ]; then
  echo "Error: No JAR file found."
  exit 1
fi

# Stop any previous ukti instance on 8082
pkill -f "education.*--server.port=8082" || true

# Wait a moment
sleep 3

# Create logs directory if needed
mkdir -p logs

# Log file
LOG_FILE="logs/ukti-$(date '+%Y-%m-%d_%H-%M-%S').log"

# Run on port 8082 with EC2 profile (RDS database)
nohup java -jar "$JAR_FILE" --server.port=8082 --spring.profiles.active=ec2 >> "$LOG_FILE" 2>&1 &

echo "✅ Ukti deployment complete. Running on port 8082."
