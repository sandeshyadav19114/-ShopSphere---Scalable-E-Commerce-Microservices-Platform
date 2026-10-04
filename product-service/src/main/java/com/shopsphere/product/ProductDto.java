package com.shopsphere.product;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
public record ProductDto(Long id, Long sellerId, String name, String description, String category, String brand, BigDecimal price) implements Serializable {
  public static ProductDto of(Product p) { return new ProductDto(p.getId(), p.getSellerId(), p.getName(), p.getDescription(), p.getCategory(), p.getBrand(), p.getPrice()); }
  public record CursorPage(List<ProductDto> items, Long nextCursor) implements Serializable {}
}
