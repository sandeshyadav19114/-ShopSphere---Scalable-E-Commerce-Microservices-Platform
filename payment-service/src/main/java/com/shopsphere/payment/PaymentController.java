package com.shopsphere.payment;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/payments") @RequiredArgsConstructor
public class PaymentController {
  private final PaymentService svc;
  @Value("${razorpay.key-id}") private String keyId;

  @GetMapping("/order/{orderId}") public Map<String, Object> checkoutParams(@PathVariable Long orderId) {
    Payment p = svc.forOrder(orderId);
    return Map.of("key", keyId, "razorpayOrderId", p.getRazorpayOrderId(), "amount", p.getAmount(), "currency", "INR", "status", p.getStatus());
  }
  /** Public route (gateway whitelists it) - authenticity is established by HMAC signature, not JWT. */
  @PostMapping("/webhook")
  public ResponseEntity<Void> webhook(@RequestBody String raw, @RequestHeader("X-Razorpay-Signature") String sig,
                                      @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId) throws Exception {
    try { svc.handleWebhook(raw, sig, eventId); return ResponseEntity.ok().build(); }
    catch (SecurityException e) { return ResponseEntity.status(400).build(); }
  }
}
