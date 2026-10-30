#!/bin/bash
# Runs automatically when LocalStack is ready. Creates the SQS queues and SNS topic.
set -euo pipefail

REGION=us-east-1
ACCOUNT=000000000000

echo "Creating SQS queues..."
# Dead-letter queues first
awslocal sqs create-queue --queue-name payment-retry-dlq --region $REGION
awslocal sqs create-queue --queue-name payment-retry-queue --region $REGION \
  --attributes '{"RedrivePolicy":"{\"deadLetterTargetArn\":\"arn:aws:sqs:'$REGION':'$ACCOUNT':payment-retry-dlq\",\"maxReceiveCount\":\"5\"}"}'

awslocal sqs create-queue --queue-name email-notification-queue --region $REGION
awslocal sqs create-queue --queue-name sms-notification-queue --region $REGION

echo "Creating SNS topic and fan-out subscriptions..."
TOPIC_ARN=$(awslocal sns create-topic --name booking-notifications --region $REGION --query TopicArn --output text)

for QUEUE in email-notification-queue sms-notification-queue; do
  awslocal sns subscribe --region $REGION \
    --topic-arn "$TOPIC_ARN" \
    --protocol sqs \
    --notification-endpoint "arn:aws:sqs:$REGION:$ACCOUNT:$QUEUE" \
    --attributes RawMessageDelivery=true
done

echo "AWS resources ready: $TOPIC_ARN"
