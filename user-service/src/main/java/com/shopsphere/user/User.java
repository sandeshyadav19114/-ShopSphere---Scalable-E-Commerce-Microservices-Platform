package com.shopsphere.user;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "users") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
  public enum Role { CUSTOMER, SELLER, ADMIN }
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  private String name;
  @Column(unique = true, nullable = false) private String email;
  private String passwordHash;           // null for social-login users
  @Enumerated(EnumType.STRING) private Role role;
  private String provider;               // LOCAL | GOOGLE
}
