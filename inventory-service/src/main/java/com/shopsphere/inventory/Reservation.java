package com.shopsphere.inventory;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;
@Entity @Table(name = "reservations") @Getter @Setter @NoArgsConstructor
public class Reservation {
  public enum State { RESERVED, COMMITTED, RELEASED }
  @Id private Long orderId;     // PK on orderId => idempotent reserve
  @Enumerated(EnumType.STRING) private State state = State.RESERVED;
  @ElementCollection(fetch = FetchType.EAGER) @CollectionTable(name = "reservation_lines", joinColumns = @JoinColumn(name = "order_id"))
  private List<Line> lines = new ArrayList<>();
  @Embeddable public record Line(Long productId, int quantity) {}
}
