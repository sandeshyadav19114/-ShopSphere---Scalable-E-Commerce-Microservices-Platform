package com.shopsphere.order;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Saga timeout: releases stock held by orders whose payment never arrived. (Use ShedLock if you run many replicas.) */
@Component @RequiredArgsConstructor
class OrderExpiryJob {
  private final OrderRepository repo; private final OrderSagaOrchestrator saga;
  @Value("${order.expiry-minutes:15}") private long minutes;
  @Scheduled(fixedDelayString = "${order.expiry-check-ms:60000}")
  void run() {
    repo.findByStatusInAndCreatedAtBefore(List.of(Order.Status.PENDING, Order.Status.INVENTORY_RESERVED), Instant.now().minus(minutes, ChronoUnit.MINUTES))
        .forEach(o -> saga.expire(o.getId()));
  }
}
