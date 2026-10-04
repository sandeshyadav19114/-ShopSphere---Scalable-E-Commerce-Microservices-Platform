package com.shopsphere.inventory;

import com.shopsphere.common.events.Events.Item;
import lombok.RequiredArgsConstructor;
import org.redisson.api.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Flash-sale safe reservation:
 *  1. Redis distributed lock per product (sorted order => no deadlocks) serialises hot-SKU contention across pods and sheds load cheaply.
 *  2. Optimistic locking (@Version) in MySQL guarantees correctness even if a lock lease expires; conflicts are retried with jitter.
 */
@Service @RequiredArgsConstructor
public class InventoryFacade {
  private final RedissonClient redisson; private final ReservationTx tx;
  private static final int MAX_RETRIES = 5;

  public void reserve(Long orderId, List<Item> items) {
    List<Long> ids = items.stream().map(Item::productId).distinct().sorted().toList();
    RLock multi = redisson.getMultiLock(ids.stream().map(id -> redisson.getLock("lock:inventory:" + id)).toArray(RLock[]::new));
    boolean locked = false;
    try {
      locked = multi.tryLock(3, 10, TimeUnit.SECONDS);
      if (!locked) throw new InsufficientStockException("System busy, please retry");
      for (int attempt = 1; ; attempt++) {
        try { tx.reserve(orderId, items); return; }
        catch (ObjectOptimisticLockingFailureException e) {
          if (attempt >= MAX_RETRIES) throw new InsufficientStockException("High contention, please retry");
          Thread.sleep(ThreadLocalRandom_jitter(attempt));
        }
      }
    } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new InsufficientStockException("Interrupted"); }
    finally { if (locked) try { multi.unlock(); } catch (IllegalMonitorStateException ignored) { /* lease expired */ } }
  }
  private long ThreadLocalRandom_jitter(int attempt) { return java.util.concurrent.ThreadLocalRandom.current().nextLong(10L * attempt, 50L * attempt); }
  public void release(Long orderId) { retry(() -> tx.release(orderId)); }
  public void commit(Long orderId) { retry(() -> tx.commit(orderId)); }
  private void retry(Runnable r) { for (int i = 0; i < MAX_RETRIES; i++) { try { r.run(); return; } catch (ObjectOptimisticLockingFailureException ignored) { } } throw new IllegalStateException("Could not update stock"); }
}
