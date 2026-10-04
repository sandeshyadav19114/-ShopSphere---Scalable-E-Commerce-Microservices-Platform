package com.shopsphere.payment;
import com.razorpay.RazorpayClient;
import com.shopsphere.common.events.Events.PaymentResult;
import com.shopsphere.common.events.Topics;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.util.HexFormat;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class PaymentWebhookTest {
  static final String SECRET = "whsec";
  PaymentRepository payments = mock(PaymentRepository.class);
  ProcessedEventRepository processed = mock(ProcessedEventRepository.class);
  KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
  PaymentService svc = new PaymentService(mock(RazorpayClient.class), payments, processed, kafka);

  PaymentWebhookTest() { ReflectionTestUtils.setField(svc, "webhookSecret", SECRET); }

  static String sign(String body) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(SECRET.getBytes(), "HmacSHA256"));
    return HexFormat.of().formatHex(mac.doFinal(body.getBytes()));
  }
  static String body(String event) { return "{\"event\":\"" + event + "\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_1\",\"order_id\":\"rz_1\"}}}}"; }
  Payment created() { Payment p = new Payment(); p.setOrderId(5L); p.setAmount(new BigDecimal("10")); p.setRazorpayOrderId("rz_1"); when(payments.findByRazorpayOrderId("rz_1")).thenReturn(Optional.of(p)); return p; }

  @Test void forgedSignatureIsRejected() {
    assertThrows(SecurityException.class, () -> svc.handleWebhook(body("payment.captured"), "deadbeef", "evt1"));
    verifyNoInteractions(kafka);
  }
  @Test void capturedEventPublishesSuccessAndMarksCaptured() throws Exception {
    Payment p = created(); String b = body("payment.captured");
    svc.handleWebhook(b, sign(b), "evt1");
    assertEquals(Payment.Status.CAPTURED, p.getStatus());
    verify(kafka).send(eq(Topics.PAYMENT_RESULT), eq("5"), eq(new PaymentResult(5L, true, "pay_1", null)));
  }
  @Test void duplicateEventIdIsIgnored() throws Exception {
    when(processed.existsById("evt1")).thenReturn(true); String b = body("payment.captured");
    svc.handleWebhook(b, sign(b), "evt1");
    verifyNoInteractions(kafka); verify(payments, never()).findByRazorpayOrderId(any());
  }
  @Test void failedAttemptDoesNotFailTheOrder() throws Exception {
    Payment p = created(); String b = body("payment.failed");
    svc.handleWebhook(b, sign(b), "evt2");
    assertEquals(Payment.Status.CREATED, p.getStatus()); verifyNoInteractions(kafka);   // customer may retry
  }
  @Test void replayedCaptureAfterRefundDoesNothing() throws Exception {
    Payment p = created(); p.setStatus(Payment.Status.REFUNDED); String b = body("payment.captured");
    svc.handleWebhook(b, sign(b), "evt3");
    verifyNoInteractions(kafka);
  }
}
