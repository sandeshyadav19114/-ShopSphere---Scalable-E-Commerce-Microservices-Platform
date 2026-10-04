package com.shopsphere.product;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

/** Composite index (category, brand, price) serves the most common filter combination; leading column is the highest-selectivity filter. */
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_cat_brand_price", columnList = "category,brand,price"),
    @Index(name = "idx_seller", columnList = "sellerId"),
    @Index(name = "idx_name", columnList = "name")})
@Getter @Setter @NoArgsConstructor
public class Product {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  private Long sellerId;
  @Column(nullable = false) private String name;
  @Column(length = 2000) private String description;
  private String category;
  private String brand;
  @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
  private Instant createdAt = Instant.now();
}
