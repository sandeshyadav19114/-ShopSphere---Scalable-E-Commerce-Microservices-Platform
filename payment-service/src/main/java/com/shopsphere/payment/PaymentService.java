package com.shopsphere.payment;

import com.razorpay.*;
import com.shopsphere.common.events.Events.*;
import com.shopsphere.common.events.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Slf4j
public class PaymentService {
  private final RazorpayClient rzp; private final PaymentRepository payments; private final ProcessedEventRepository processed;
  private final KafkaTemplate<String, Object> kafka;
  @Value("${razorpay.webhook-secret}") private String webhookSecret;
  @Value("${razorpay.mock:false}") private boolean mock;   // local dev: skip Razorpay API calls (webhooks still signature-verified)

  /** Saga command: create the Razorpay order. Idempotent on orderId, so Kafka redelivery never double-creates. */
  @KafkaListener(topics = Topics.PAYMENT_REQUEST_CMD)
  @Transactional public void onPaymentRequest(PaymentRequest r) throws RazorpayException {
    if (payments.findByOrderId(r.orderId()).isPresent()) return;
    String rzpOrderId;
    if (mock) rzpOrderId = "mock_" + r.orderId();
    else {
      JSONObject req = new JSONObject();
      req.put("amount", r.amount().movePointRight(2).longValueExact());   // paise
      req.put("currency", "INR"); req.put("receipt", "order_" + r.orderId());
      rzpOrderId = ((com.razorpay.Order) rzp.orders.create(req)).get("id");
    }
    Payment p = new Payment(); p.setOrderId(r.orderId()); p.setAmount(r.amount()); p.setRazorpayOrderId(rzpOrderId); payments.save(p);
  }

  public Payment forOrder(Long orderId) { return payments.findByOrderId(orderId).orElseThrow(); }

  /** Webhook: signature-verified over the RAW body, de-duplicated by X-Razorpay-Event-Id. */
  @Transactional public void handleWebhook(String body, String signature, String eventId) throws RazorpayException {
    if (!Utils.verifyWebhookSignature(body, signature, webhookSecret)) throw new SecurityException("Invalid signature");
    if (eventId != null && processed.existsById(eventId)) return;
    if (eventId != null) processed.save(new ProcessedEvent(eventId));
    JSONObject json = new JSONObject(body);
    String event = json.getString("event");
    JSONObject entity = json.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
    Payment p = payments.findByRazorpayOrderId(entity.getString("order_id")).orElse(null);
    if (p == null) { log.warn("Webhook for unknown order {}", entity.getString("order_id")); return; }
    switch (event) {
      case "payment.captured" -> {
        if (p.getStatus() != Payment.Status.CREATED) return;
        p.setStatus(Payment.Status.CAPTURED); p.setRazorpayPaymentId(entity.getString("id"));
        kafka.send(Topics.PAYMENT_RESULT, String.valueOf(p.getOrderId()), new PaymentResult(p.getOrderId(), true, p.getRazorpayPaymentId(), null));
      }
      // A failed ATTEMPT is not a failed ORDER: Razorpay Checkout lets the customer retry on the same order.
      // The order saga times out unpaid orders instead (OrderExpiryJob).
      case "payment.failed" -> log.info("Payment attempt failed for order {}: {}", p.getOrderId(), entity.optString("error_description"));
      default -> log.info("Ignored event {}", event);
    }
  }

  /** Automated refund when a paid order is cancelled, or paid too late. */
  @KafkaListener(topics = Topics.PAYMENT_REFUND_CMD)
  @Transactional public void refund(RefundCmd c) throws RazorpayException {
    Payment p = payments.findByOrderId(c.orderId()).orElseThrow();
    if (p.getStatus() != Payment.Status.CAPTURED) return;                 // idempotent
    if (mock) p.setRefundId("mock_refund_" + c.orderId());
    else {
      JSONObject req = new JSONObject(); req.put("amount", p.getAmount().movePointRight(2).longValueExact());
      p.setRefundId(((com.razorpay.Refund) rzp.payments.refund(p.getRazorpayPaymentId(), req)).get("id"));
    }
    p.setStatus(Payment.Status.REFUNDED);
  }
}
