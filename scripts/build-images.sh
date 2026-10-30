#!/bin/bash
# Builds one Docker image per service and (optionally) loads them into minikube.
# Usage: ./scripts/build-images.sh            -> build only
#        ./scripts/build-images.sh minikube   -> build and load into minikube
set -euo pipefail
cd "$(dirname "$0")/.."

SERVICES="api-gateway user-service catalog-service booking-service payment-service notification-service"
for s in $SERVICES; do
  echo ">>> Building $s"
  docker build --build-arg SERVICE="$s" -t "ticket-booking/$s:1.0" .
  if [ "${1:-}" = "minikube" ]; then
    minikube image load "ticket-booking/$s:1.0"
  fi
done
echo "Done."
