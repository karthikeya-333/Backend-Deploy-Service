#!/bin/sh
set -e 

echo "Cloning Spring Boot repository: $GITHUB_URL"
git clone "$GITHUB_URL" .

echo "Building application..."
mvn clean package -DskipTests

JAR_FILE=$(ls target/*.jar | head -n 1)

echo "Starting Spring Boot on port: $SERVER_PORT"
exec java -jar "$JAR_FILE" --server.port=$SERVER_PORT