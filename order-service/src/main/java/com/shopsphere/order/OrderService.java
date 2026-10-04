package com.shopsphere.order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service @RequiredArgsConstructor
public class OrderService {
  public record Line(Long productId, int quantity) {}
  public record CreateOrder(List<Line> items, String shippingAddress) {}
  private final OrderRepository repo; private final ProductClient products; private final OrderSagaOrchestrator saga;

  public Order create(Long userId, String email, CreateOrder req) {
    Order o = new Order(); o.setUserId(userId); o.setEmail(email); o.setShippingAddress(req.shippingAddress());
    BigDecimal total = BigDecimal.ZERO;
    for (Line l : req.items()) {                       // prices always come from catalog, never from the client
      var p = products.get(l.productId());
      o.getLines().add(new Order.OrderLine(null, p.id(), p.name(), l.quantity(), p.price()));
      total = total.add(p.price().multiply(BigDecimal.valueOf(l.quantity())));
    }
    o.setTotal(total);
    Order saved = repo.save(o);
    saga.start(saved);
    return saved;
  }
  @Transactional public Order advance(Long id, Order.Status next) {
    Order o = repo.findById(id).orElseThrow();
    var allowed = switch (o.getStatus()) { case CONFIRMED -> Order.Status.SHIPPED; case SHIPPED -> Order.Status.OUT_FOR_DELIVERY; case OUT_FOR_DELIVERY -> Order.Status.DELIVERED; default -> null; };
    if (allowed != next) throw new IllegalStateException("Illegal transition " + o.getStatus() + " -> " + next);
    o.setStatus(next); o.setUpdatedAt(java.time.Instant.now()); saga.publish(o); return o;
  }
}
