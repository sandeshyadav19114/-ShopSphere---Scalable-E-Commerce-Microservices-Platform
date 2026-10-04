package com.shopsphere.inventory;
import com.shopsphere.common.events.Events.*;
import com.shopsphere.common.events.Topics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class InventoryListener {
  private final InventoryFacade facade; private final KafkaTemplate<String, Object> kafka;

  @KafkaListener(topics = Topics.INVENTORY_RESERVE_CMD)
  public void reserve(ReserveInventoryCmd c) {
    try { facade.reserve(c.orderId(), c.items()); kafka.send(Topics.INVENTORY_RESERVED, String.valueOf(c.orderId()), new InventoryReserved(c.orderId())); }
    catch (InsufficientStockException e) { kafka.send(Topics.INVENTORY_FAILED, String.valueOf(c.orderId()), new InventoryFailed(c.orderId(), e.getMessage())); }
  }
  @KafkaListener(topics = Topics.INVENTORY_RELEASE_CMD) public void release(ReleaseInventoryCmd c) { facade.release(c.orderId()); }
  @KafkaListener(topics = Topics.ORDER_EVENTS) public void onOrder(OrderEvent e) { if ("CONFIRMED".equals(e.status())) facade.commit(e.orderId()); }
}
