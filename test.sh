#!/bin/sh

echo "Running tests..."
docker build --build-arg TEST_API_KEY=$TEST_API_KEY .
