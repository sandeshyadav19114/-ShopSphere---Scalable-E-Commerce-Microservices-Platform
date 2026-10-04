package com.shopsphere.payment;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity @Table(name = "payments", uniqueConstraints = @UniqueConstraint(columnNames = "orderId")) @Getter @Setter @NoArgsConstructor
public class Payment {
  public enum Status { CREATED, CAPTURED, FAILED, REFUNDED }
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  private Long orderId;                        // unique => one payment per order (idempotency key)
  @Column(unique = true) private String razorpayOrderId;
  private String razorpayPaymentId; private String refundId;
  private BigDecimal amount; @Enumerated(EnumType.STRING) private Status status = Status.CREATED;
}
