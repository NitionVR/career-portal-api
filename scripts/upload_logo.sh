#!/bin/bash

# S3 bucket name (replace with your actual bucket name if different)
BUCKET_NAME="etalente-uploads-970835057946"
REGION="af-south-1"
LOCAL_FILE="logo.png"
S3_KEY="avatars/logo.png"
CONTENT_TYPE="image/png"
ACL="public-read" # To make the uploaded object publicly readable

echo "Uploading ${LOCAL_FILE} to s3://${BUCKET_NAME}/${S3_KEY}..."

aws s3 cp "${LOCAL_FILE}" "s3://${BUCKET_NAME}/${S3_KEY}" \
  --region "${REGION}" \
  --acl "${ACL}" \
  --content-type "${CONTENT_TYPE}"

if [ $? -eq 0 ]; then
  echo "Upload successful!"
  echo "Public URL: https://${BUCKET_NAME}.s3.${REGION}.amazonaws.com/${S3_KEY}"
else
  echo "Upload failed."
fi
