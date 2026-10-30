#!/bin/bash
# End-to-end happy path through the API gateway. Requires curl and jq.
# Usage: ./scripts/demo.sh [gateway-url]   (default http://localhost:8080)
set -euo pipefail
GW=${1:-http://localhost:8080}
EMAIL="user$RANDOM@example.com"

echo "1) Register"
TOKEN=$(curl -s -X POST "$GW/api/auth/register" -H 'Content-Type: application/json' \
  -d "{\"name\":\"Demo User\",\"email\":\"$EMAIL\",\"password\":\"secret123\"}" | jq -r .token)
echo "   token: ${TOKEN:0:25}..."

echo "2) Browse movies and shows (public, served from Redis cache after the first call)"
curl -s "$GW/api/movies" | jq .
curl -s "$GW/api/movies/1/shows" | jq .

echo "3) Book seats A1, A2 for show 1"
REF=$(curl -s -X POST "$GW/api/bookings" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"showId":1,"seatNumbers":["A1","A2"]}' | tee /dev/stderr | jq -r .bookingRef)

echo "4) Try to grab the same seats as another request (expect 409 Conflict)"
curl -s -o /dev/null -w "   HTTP %{http_code}\n" -X POST "$GW/api/bookings" -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"showId":1,"seatNumbers":["A2","A3"]}'

echo "5) Wait for the payment Saga, then check the booking"
for i in 1 2 3 4 5 6 7 8 9 10; do
  STATUS=$(curl -s "$GW/api/bookings/$REF" -H "Authorization: Bearer $TOKEN" | jq -r .status)
  echo "   status: $STATUS"
  [ "$STATUS" != "PENDING" ] && break
  sleep 5
done

echo "6) Seat map now"
curl -s "$GW/api/shows/1/seats" | jq -c '.seats[:5]'
echo "Check notification-service logs for the [EMAIL] and [SMS] lines."
