package com.shopsphere.order;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController @RequestMapping("/api/orders") @RequiredArgsConstructor
public class OrderController {
  private final OrderService svc; private final OrderRepository repo; private final OrderSagaOrchestrator saga;

  @PostMapping @ResponseStatus(HttpStatus.ACCEPTED)  // 202: saga continues asynchronously
  public Order create(@RequestHeader("X-User-Id") Long uid, @RequestHeader("X-User-Email") String email, @RequestBody OrderService.CreateOrder r) { return svc.create(uid, email, r); }

  @GetMapping public List<Order> mine(@RequestHeader("X-User-Id") Long uid) { return repo.findByUserIdOrderByCreatedAtDesc(uid); }

  @GetMapping("/{id}") public Order get(@PathVariable Long id, @RequestHeader("X-User-Id") Long uid, @RequestHeader("X-User-Role") String role) { return owned(id, uid, role); }

  @PostMapping("/{id}/cancel") public Order cancel(@PathVariable Long id, @RequestHeader("X-User-Id") Long uid, @RequestHeader("X-User-Role") String role) {
    Order o = owned(id, uid, role); saga.customerCancel(o); return repo.save(o);
  }
  /** Delivery tracking: SELLER/ADMIN move the order CONFIRMED -> SHIPPED -> OUT_FOR_DELIVERY -> DELIVERED. */
  @PatchMapping("/{id}/shipment") public Order ship(@PathVariable Long id, @RequestParam Order.Status status, @RequestHeader("X-User-Role") String role) {
    if (role.equals("CUSTOMER")) throw new SecurityException("Forbidden");
    return svc.advance(id, status);
  }
  @GetMapping("/{id}/invoice") public Map<String, Object> invoice(@PathVariable Long id, @RequestHeader("X-User-Id") Long uid, @RequestHeader("X-User-Role") String role) {
    Order o = owned(id, uid, role);
    if (o.getStatus() == Order.Status.PENDING || o.getStatus() == Order.Status.CANCELLED) throw new IllegalStateException("Invoice available only for confirmed orders");
    BigDecimal tax = o.getTotal().multiply(new BigDecimal("0.18")).setScale(2, java.math.RoundingMode.HALF_UP); // GST 18% (illustrative, tax-inclusive split)
    return Map.of("invoiceNo", "INV-" + String.format("%08d", o.getId()), "orderId", o.getId(), "issuedAt", o.getUpdatedAt(), "lines", o.getLines(),
        "total", o.getTotal(), "taxIncluded", tax, "billTo", o.getEmail(), "shipTo", o.getShippingAddress());
  }
  private Order owned(Long id, Long uid, String role) {
    Order o = repo.findById(id).orElseThrow(() -> new NoSuchElementException("Order not found"));
    if (!o.getUserId().equals(uid) && role.equals("CUSTOMER")) throw new SecurityException("Forbidden");
    return o;
  }
  @ExceptionHandler(NoSuchElementException.class) ResponseEntity<String> nf(Exception e) { return ResponseEntity.status(404).body(e.getMessage()); }
  @ExceptionHandler(SecurityException.class) ResponseEntity<String> fb(Exception e) { return ResponseEntity.status(403).body(e.getMessage()); }
  @ExceptionHandler(IllegalStateException.class) ResponseEntity<String> bad(Exception e) { return ResponseEntity.status(409).body(e.getMessage()); }
}
