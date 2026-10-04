package com.shopsphere.order;
import com.shopsphere.common.events.Events.*;
import com.shopsphere.common.events.Topics;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class OrderSagaOrchestratorTest {
  OrderRepository repo = mock(OrderRepository.class);
  KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
  OrderSagaOrchestrator saga = new OrderSagaOrchestrator(repo, kafka);

  Order order(Order.Status st) {
    Order o = new Order(); o.setId(1L); o.setUserId(7L); o.setEmail("a@b.com"); o.setTotal(new BigDecimal("100.00")); o.setStatus(st);
    when(repo.findById(1L)).thenReturn(Optional.of(o)); return o;
  }

  @Test void reservedOrderRequestsPaymentOnce() {
    Order o = order(Order.Status.PENDING);
    saga.onReserved(new InventoryReserved(1L)); saga.onReserved(new InventoryReserved(1L));   // duplicate delivery
    assertEquals(Order.Status.INVENTORY_RESERVED, o.getStatus());
    verify(kafka, times(1)).send(eq(Topics.PAYMENT_REQUEST_CMD), eq("1"), any(PaymentRequest.class));
  }
  @Test void reservationFailureCancelsWithoutRelease() {
    Order o = order(Order.Status.PENDING);
    saga.onReserveFailed(new InventoryFailed(1L, "Insufficient stock"));
    assertEquals(Order.Status.CANCELLED, o.getStatus());
    verify(kafka, never()).send(eq(Topics.INVENTORY_RELEASE_CMD), anyString(), any());
  }
  @Test void successfulPaymentConfirmsAndNotifies() {
    Order o = order(Order.Status.INVENTORY_RESERVED);
    saga.onPayment(new PaymentResult(1L, true, "pay_1", null));
    assertEquals(Order.Status.CONFIRMED, o.getStatus());
    verify(kafka).send(eq(Topics.ORDER_EVENTS), eq("1"), any(OrderEvent.class));
  }
  @Test void failedPaymentCompensatesByReleasingStock() {
    Order o = order(Order.Status.INVENTORY_RESERVED);
    saga.onPayment(new PaymentResult(1L, false, null, "declined"));
    assertEquals(Order.Status.CANCELLED, o.getStatus());
    verify(kafka).send(eq(Topics.INVENTORY_RELEASE_CMD), eq("1"), any(ReleaseInventoryCmd.class));
  }
  @Test void latePaymentOnCancelledOrderIsRefunded() {
    Order o = order(Order.Status.CANCELLED);
    saga.onPayment(new PaymentResult(1L, true, "pay_1", null));
    assertEquals(Order.Status.CANCELLED, o.getStatus());
    verify(kafka).send(eq(Topics.PAYMENT_REFUND_CMD), eq("1"), any(RefundCmd.class));
  }
  @Test void expiryCancelsUnpaidOrderAndReleasesStock() {
    Order o = order(Order.Status.INVENTORY_RESERVED);
    saga.expire(1L);
    assertEquals(Order.Status.CANCELLED, o.getStatus());
    verify(kafka).send(eq(Topics.INVENTORY_RELEASE_CMD), eq("1"), any(ReleaseInventoryCmd.class));
  }
  @Test void paidCustomerCancellationTriggersRefundAndRelease() {
    Order o = order(Order.Status.CONFIRMED);
    saga.customerCancel(o);
    verify(kafka).send(eq(Topics.PAYMENT_REFUND_CMD), eq("1"), any(RefundCmd.class));
    verify(kafka).send(eq(Topics.INVENTORY_RELEASE_CMD), eq("1"), any(ReleaseInventoryCmd.class));
  }
}
