#!/bin/sh
set -e 

echo "Cloning repository: $GITHUB_URL"
git clone "$GITHUB_URL" .

echo "Installing dependencies..."
npm install

echo "Starting application on port: $PORT"

exec env PORT=$PORT npm start