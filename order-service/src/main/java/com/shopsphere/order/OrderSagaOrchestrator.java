package com.shopsphere.order;

import com.shopsphere.common.events.Events.*;
import com.shopsphere.common.events.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import static com.shopsphere.order.Order.Status.*;

/**
 * Orchestration saga (no distributed locks, no 2PC):
 *   PENDING -> reserve inventory -> INVENTORY_RESERVED -> payment captured -> CONFIRMED
 * Compensations:
 *   reservation failed            -> CANCELLED
 *   payment failed / timed out    -> release inventory + CANCELLED
 *   payment captured after cancel -> automatic refund (late-payment protection)
 * Every handler is idempotent (state check) because Kafka delivers at-least-once.
 */
@Component @RequiredArgsConstructor @Slf4j
public class OrderSagaOrchestrator {
  private final OrderRepository repo; private final KafkaTemplate<String, Object> kafka;

  public void start(Order o) {
    kafka.send(Topics.INVENTORY_RESERVE_CMD, String.valueOf(o.getId()),
        new ReserveInventoryCmd(o.getId(), o.getLines().stream().map(l -> new Item(l.getProductId(), l.getQuantity())).toList()));
  }

  @KafkaListener(topics = Topics.INVENTORY_RESERVED)
  @Transactional public void onReserved(InventoryReserved e) {
    Order o = repo.findById(e.orderId()).orElseThrow();
    if (o.getStatus() != PENDING) return;
    o.setStatus(INVENTORY_RESERVED);
    kafka.send(Topics.PAYMENT_REQUEST_CMD, String.valueOf(o.getId()), new PaymentRequest(o.getId(), o.getUserId(), o.getTotal()));
  }

  @KafkaListener(topics = Topics.INVENTORY_FAILED)
  @Transactional public void onReserveFailed(InventoryFailed e) { cancel(repo.findById(e.orderId()).orElseThrow(), e.reason(), false); }

  @KafkaListener(topics = Topics.PAYMENT_RESULT)
  @Transactional public void onPayment(PaymentResult e) {
    Order o = repo.findById(e.orderId()).orElseThrow();
    if (o.getStatus() == CANCELLED && e.success()) {            // customer paid after we gave up (timeout) -> money back
      log.warn("Late payment for cancelled order {} - refunding", o.getId());
      kafka.send(Topics.PAYMENT_REFUND_CMD, String.valueOf(o.getId()), new RefundCmd(o.getId()));
      return;
    }
    if (o.getStatus() != INVENTORY_RESERVED) return;            // duplicate delivery
    if (e.success()) { o.setStatus(CONFIRMED); publish(o); }
    else cancel(o, "Payment failed: " + e.reason(), true);
  }

  /** Customer cancellation: allowed until shipped. Paid orders are refunded automatically. */
  @Transactional public void customerCancel(Order o) {
    if (o.getStatus() == CONFIRMED) { kafka.send(Topics.PAYMENT_REFUND_CMD, String.valueOf(o.getId()), new RefundCmd(o.getId())); cancel(o, "Cancelled by customer", true); }
    else if (o.getStatus() == PENDING || o.getStatus() == INVENTORY_RESERVED) cancel(o, "Cancelled by customer", true);
    else throw new IllegalStateException("Cannot cancel order in status " + o.getStatus());
  }

  /** Called by the expiry job for orders stuck before payment. */
  @Transactional public void expire(Long orderId) {
    repo.findById(orderId).filter(o -> o.getStatus() == PENDING || o.getStatus() == INVENTORY_RESERVED)
        .ifPresent(o -> cancel(o, "Payment not completed in time", true));
  }

  private void cancel(Order o, String reason, boolean release) {
    if (o.getStatus() == CANCELLED) return;
    o.setStatus(CANCELLED); o.setCancelReason(reason);
    if (release) kafka.send(Topics.INVENTORY_RELEASE_CMD, String.valueOf(o.getId()), new ReleaseInventoryCmd(o.getId()));
    publish(o);
  }
  void publish(Order o) { kafka.send(Topics.ORDER_EVENTS, String.valueOf(o.getId()), new OrderEvent(o.getId(), o.getUserId(), o.getEmail(), o.getStatus().name(), o.getTotal())); }
}
