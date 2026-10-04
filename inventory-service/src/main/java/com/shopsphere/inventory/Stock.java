package com.shopsphere.inventory;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "stock") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Stock {
  @Id private Long productId;
  private int available;
  private int reserved;
  @Version private long version;   // optimistic lock: last line of defence even if the Redis lock expires mid-operation
}
