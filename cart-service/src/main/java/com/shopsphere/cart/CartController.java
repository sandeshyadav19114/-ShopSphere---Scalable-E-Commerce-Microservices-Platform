package com.shopsphere.cart;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.*;

/** Cart lives in Redis (hash per user, 7-day sliding TTL). Checkout hands off to Order Service via OpenFeign. */
@RestController @RequestMapping("/api/cart") @RequiredArgsConstructor
public class CartController {
  public record AddReq(Long productId, int quantity) {}
  public record CheckoutReq(String shippingAddress) {}
  private final StringRedisTemplate redis; private final OrderClient orders;
  private String key(String uid) { return "cart:" + uid; }

  @GetMapping public Map<Object, Object> get(@RequestHeader("X-User-Id") String uid) { return redis.opsForHash().entries(key(uid)); }

  @PostMapping("/items")
  public Map<Object, Object> add(@RequestHeader("X-User-Id") String uid, @RequestBody AddReq r) {
    if (r.quantity() <= 0) throw new IllegalArgumentException("quantity must be > 0");
    redis.opsForHash().increment(key(uid), String.valueOf(r.productId()), r.quantity());
    redis.expire(key(uid), Duration.ofDays(7));
    return get(uid);
  }
  @DeleteMapping("/items/{productId}")
  public Map<Object, Object> remove(@RequestHeader("X-User-Id") String uid, @PathVariable String productId) { redis.opsForHash().delete(key(uid), productId); return get(uid); }

  @PostMapping("/checkout")
  public Map<String, Object> checkout(@RequestHeader("X-User-Id") String uid, @RequestHeader("X-User-Email") String email, @RequestBody CheckoutReq r) {
    var cart = get(uid);
    if (cart.isEmpty()) throw new IllegalStateException("Cart is empty");
    List<OrderClient.Line> lines = cart.entrySet().stream().map(e -> new OrderClient.Line(Long.valueOf((String) e.getKey()), Integer.parseInt((String) e.getValue()))).toList();
    Map<String, Object> order = orders.create(uid, email, new OrderClient.CreateOrder(lines, r.shippingAddress()));
    redis.delete(key(uid));
    return order;
  }
}
