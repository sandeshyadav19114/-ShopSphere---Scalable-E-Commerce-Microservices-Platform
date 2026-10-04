package com.shopsphere.inventory;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/inventory") @RequiredArgsConstructor
public class StockController {
  private final StockRepository repo;
  @GetMapping("/{productId}") public Stock get(@PathVariable Long productId) { return repo.findById(productId).orElseThrow(); }
  /** SELLER/ADMIN (gateway-enforced): set absolute stock level. */
  @PutMapping("/{productId}") public Stock set(@PathVariable Long productId, @RequestParam int available) {
    Stock s = repo.findById(productId).orElseGet(() -> new Stock(productId, 0, 0, 0));
    s.setAvailable(available); return repo.save(s);
  }
}
