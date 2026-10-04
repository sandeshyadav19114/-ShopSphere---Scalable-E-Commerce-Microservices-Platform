package com.shopsphere.cart;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
@FeignClient(name = "ORDER-SERVICE")
public interface OrderClient {
  record Line(Long productId, int quantity) {}
  record CreateOrder(List<Line> items, String shippingAddress) {}
  @PostMapping("/api/orders")
  Map<String, Object> create(@RequestHeader("X-User-Id") String uid, @RequestHeader("X-User-Email") String email, @RequestBody CreateOrder body);
}
