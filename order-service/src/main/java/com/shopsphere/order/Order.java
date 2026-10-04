package com.shopsphere.order;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity @Table(name = "orders", indexes = @Index(name = "idx_orders_user", columnList = "userId,createdAt"))
@Getter @Setter @NoArgsConstructor
public class Order {
  public enum Status { PENDING, INVENTORY_RESERVED, CONFIRMED, SHIPPED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED }
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  private Long userId; private String email; private String shippingAddress;
  @Enumerated(EnumType.STRING) private Status status = Status.PENDING;
  private BigDecimal total; private String cancelReason;
  private Instant createdAt = Instant.now(); private Instant updatedAt = Instant.now();
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER) @JoinColumn(name = "order_id") private List<OrderLine> lines = new ArrayList<>();

  @Entity @Table(name = "order_lines") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
  public static class OrderLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long productId; private String productName; private int quantity; private BigDecimal unitPrice;
  }
}
