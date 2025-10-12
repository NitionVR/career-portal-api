#!/bin/bash

# Base URL of your Spring Boot application
BASE_URL="http://localhost:8080"

# Endpoint for listing public job posts
# Adding sorting by 'createdAt' in descending order
ENDPOINT="/api/job-posts?sort=createdAt,desc"

# Output file for the JSON results
OUTPUT_FILE="public_job_posts.json"

echo "Testing public job posts endpoint: GET ${BASE_URL}${ENDPOINT}"
echo "----------------------------------------------------"

# Make the GET request and save the response to a file
HTTP_STATUS=$(curl -X GET "${BASE_URL}${ENDPOINT}" \
                   -H "Accept: application/json" \
                   -o "${OUTPUT_FILE}" \
                   -w "%\{http_code}")

echo "HTTP Status: ${HTTP_STATUS}"
echo "Results saved to: ${OUTPUT_FILE}"
echo "----------------------------------------------------"
echo "Content of ${OUTPUT_FILE}:"
cat "${OUTPUT_FILE}"
echo "----------------------------------------------------"
echo "Test complete."