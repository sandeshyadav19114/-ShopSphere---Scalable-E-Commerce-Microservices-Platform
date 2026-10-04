package com.shopsphere.product;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service @RequiredArgsConstructor
public class ProductService {
  private final ProductRepository repo;

  @Cacheable(value = "product", key = "#id")
  public ProductDto get(Long id) { return repo.findById(id).map(ProductDto::of).orElseThrow(() -> new IllegalArgumentException("Product not found")); }

  @Cacheable(value = "productList", key = "T(java.util.Objects).hash(#cursor,#category,#brand,#min,#max,#q,#limit)")
  public ProductDto.CursorPage list(long cursor, String category, String brand, BigDecimal min, BigDecimal max, String q, int limit) {
    int size = Math.min(Math.max(limit, 1), 50);
    List<Product> rows = repo.page(cursor, category, brand, min, max, q, PageRequest.of(0, size));
    Long next = rows.size() == size ? rows.get(rows.size() - 1).getId() : null;
    return new ProductDto.CursorPage(rows.stream().map(ProductDto::of).toList(), next);
  }

  @Transactional @CacheEvict(value = "productList", allEntries = true)
  public ProductDto create(Long sellerId, ProductDto d) {
    Product p = new Product(); apply(p, d); p.setSellerId(sellerId); return ProductDto.of(repo.save(p));
  }

  @Transactional @Caching(evict = { @CacheEvict(value = "product", key = "#id"), @CacheEvict(value = "productList", allEntries = true) })
  public ProductDto update(Long id, Long userId, String role, ProductDto d) {
    Product p = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found"));
    if (!"ADMIN".equals(role) && !p.getSellerId().equals(userId)) throw new SecurityException("Not your product");
    apply(p, d); return ProductDto.of(p);
  }

  @Transactional @Caching(evict = { @CacheEvict(value = "product", key = "#id"), @CacheEvict(value = "productList", allEntries = true) })
  public void delete(Long id, Long userId, String role) {
    Product p = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found"));
    if (!"ADMIN".equals(role) && !p.getSellerId().equals(userId)) throw new SecurityException("Not your product");
    repo.delete(p);
  }
  private void apply(Product p, ProductDto d) {
    p.setName(d.name()); p.setDescription(d.description()); p.setCategory(d.category()); p.setBrand(d.brand()); p.setPrice(d.price());
  }
}
