#!/bin/bash
# LocalStack runs this automatically on startup (mounted into
# /etc/localstack/init/ready.d/) because unlike a real S3 account, a fresh
# LocalStack container has no buckets at all — core-api would otherwise fail
# every upload with NoSuchBucket the first time it runs against a clean stack.
set -e

awslocal s3 mb "s3://${S3_BUCKET_NAME:-traintrack-certificates}"
