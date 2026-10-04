package com.shopsphere.product;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController @RequestMapping("/api/products") @RequiredArgsConstructor
public class ProductController {
  private final ProductService svc;

  @GetMapping public ProductDto.CursorPage list(@RequestParam(defaultValue = "0") long cursor, @RequestParam(required = false) String category,
      @RequestParam(required = false) String brand, @RequestParam(required = false) BigDecimal minPrice, @RequestParam(required = false) BigDecimal maxPrice,
      @RequestParam(required = false) String q, @RequestParam(defaultValue = "20") int limit) {
    return svc.list(cursor, category, brand, minPrice, maxPrice, q, limit);
  }
  @GetMapping("/{id}") public ProductDto get(@PathVariable Long id) { return svc.get(id); }
  @PostMapping @ResponseStatus(HttpStatus.CREATED)
  public ProductDto create(@RequestHeader("X-User-Id") Long uid, @RequestBody ProductDto d) { return svc.create(uid, d); }
  @PutMapping("/{id}")
  public ProductDto update(@PathVariable Long id, @RequestHeader("X-User-Id") Long uid, @RequestHeader("X-User-Role") String role, @RequestBody ProductDto d) { return svc.update(id, uid, role, d); }
  @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id, @RequestHeader("X-User-Id") Long uid, @RequestHeader("X-User-Role") String role) { svc.delete(id, uid, role); }

  @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<String> nf(IllegalArgumentException e) { return ResponseEntity.status(404).body(e.getMessage()); }
  @ExceptionHandler(SecurityException.class) ResponseEntity<String> forbidden(SecurityException e) { return ResponseEntity.status(403).body(e.getMessage()); }
}
