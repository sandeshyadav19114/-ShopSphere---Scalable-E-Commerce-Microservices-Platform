#!/usr/bin/env bash
# End-to-end happy path + cancellation. Needs: docker compose up (RAZORPAY_MOCK=true), curl, jq, openssl.
set -euo pipefail
GW=${GW:-http://localhost:8080}; WH_SECRET=${RAZORPAY_WEBHOOK_SECRET:-whsec}
j(){ curl -sf -H 'Content-Type: application/json' "$@"; }
EMAIL="seller$RANDOM@shop.com"
echo "1. register seller";   TOKEN=$(j -XPOST $GW/api/auth/register -d "{\"name\":\"Sam\",\"email\":\"$EMAIL\",\"password\":\"password1\",\"role\":\"SELLER\"}" | jq -r .token)
A=(-H "Authorization: Bearer $TOKEN")
echo "2. create product";    PID=$(j "${A[@]}" -XPOST $GW/api/products -d '{"name":"Phone X","category":"mobile","brand":"Acme","price":19999}' | jq -r .id)
echo "3. stock = 5";         j "${A[@]}" -XPUT "$GW/api/inventory/$PID?available=5" >/dev/null
echo "4. add to cart";       j "${A[@]}" -XPOST $GW/api/cart/items -d "{\"productId\":$PID,\"quantity\":2}" >/dev/null
echo "5. checkout";          OID=$(j "${A[@]}" -XPOST $GW/api/cart/checkout -d '{"shippingAddress":"Jaipur"}' | jq -r .id)
echo "   order $OID - waiting for saga (inventory -> payment request)"; sleep 6
echo "6. payment params";    j "${A[@]}" $GW/api/payments/order/$OID | jq -c .
echo "7. stock after reserve"; j "${A[@]}" $GW/api/inventory/$PID | jq -c '{available,reserved}'
BODY="{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_mock_$OID\",\"order_id\":\"mock_$OID\"}}}}"
SIG=$(printf '%s' "$BODY" | openssl dgst -sha256 -hmac "$WH_SECRET" -hex | awk '{print $NF}')
echo "8. signed webhook";    curl -sf -XPOST $GW/api/payments/webhook -H 'Content-Type: application/json' -H "X-Razorpay-Signature: $SIG" -H "X-Razorpay-Event-Id: evt_$OID" -d "$BODY" && echo " ok"
echo "   forged webhook should be rejected:"; curl -s -o /dev/null -w "   HTTP %{http_code}\n" -XPOST $GW/api/payments/webhook -H 'Content-Type: application/json' -H "X-Razorpay-Signature: bad" -d "$BODY"
sleep 4
echo "9. order status";      j "${A[@]}" $GW/api/orders/$OID | jq -r .status
echo "10. ship -> deliver";  for s in SHIPPED OUT_FOR_DELIVERY DELIVERED; do j "${A[@]}" -XPATCH "$GW/api/orders/$OID/shipment?status=$s" | jq -r .status; done
echo "11. invoice";          j "${A[@]}" $GW/api/orders/$OID/invoice | jq -c '{invoiceNo,total,taxIncluded}'
echo "12. oversell check - ordering 10 when only 3 remain should be CANCELLED"
j "${A[@]}" -XPOST $GW/api/cart/items -d "{\"productId\":$PID,\"quantity\":10}" >/dev/null
OID2=$(j "${A[@]}" -XPOST $GW/api/cart/checkout -d '{"shippingAddress":"Jaipur"}' | jq -r .id); sleep 5
j "${A[@]}" $GW/api/orders/$OID2 | jq -c '{status,cancelReason}'
echo "Smoke test finished."
