package com.shopsphere.inventory;
import com.shopsphere.common.events.Events.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** Transactional unit of work. Called while the Redis locks are held by InventoryFacade. All-or-nothing across lines. */
@Service @RequiredArgsConstructor
class ReservationTx {
  private final StockRepository stock; private final ReservationRepository reservations;

  @Transactional void reserve(Long orderId, List<Item> items) {
    if (reservations.existsById(orderId)) return;                       // idempotent replay
    Reservation r = new Reservation();
    r.setOrderId(orderId);
    for (Item i : items) {
      Stock s = stock.findById(i.productId()).orElseThrow(() -> new InsufficientStockException("Unknown product " + i.productId()));
      if (s.getAvailable() < i.quantity()) throw new InsufficientStockException("Insufficient stock for product " + i.productId());
      s.setAvailable(s.getAvailable() - i.quantity()); s.setReserved(s.getReserved() + i.quantity());
      r.getLines().add(new Reservation.Line(i.productId(), i.quantity()));
    }
    reservations.save(r);
  }
  @Transactional void release(Long orderId) {
    reservations.findById(orderId).filter(r -> r.getState() == Reservation.State.RESERVED).ifPresent(r -> {
      r.getLines().forEach(l -> { Stock s = stock.findById(l.productId()).orElseThrow(); s.setAvailable(s.getAvailable() + l.quantity()); s.setReserved(s.getReserved() - l.quantity()); });
      r.setState(Reservation.State.RELEASED);
    });
  }
  @Transactional void commit(Long orderId) {
    reservations.findById(orderId).filter(r -> r.getState() == Reservation.State.RESERVED).ifPresent(r -> {
      r.getLines().forEach(l -> { Stock s = stock.findById(l.productId()).orElseThrow(); s.setReserved(s.getReserved() - l.quantity()); });
      r.setState(Reservation.State.COMMITTED);
    });
  }
}
