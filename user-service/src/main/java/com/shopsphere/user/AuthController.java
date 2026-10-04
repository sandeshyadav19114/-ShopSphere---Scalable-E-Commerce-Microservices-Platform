package com.shopsphere.user;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequiredArgsConstructor
public class AuthController {
  public record RegisterReq(@NotBlank String name, @Email String email, @Size(min = 8) String password, User.Role role) {}
  public record LoginReq(@Email String email, @NotBlank String password) {}
  private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt;

  @PostMapping("/api/auth/register")
  public ResponseEntity<?> register(@Valid @RequestBody RegisterReq r) {
    if (users.findByEmail(r.email()).isPresent()) return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Email already registered"));
    User.Role role = r.role() == User.Role.SELLER ? User.Role.SELLER : User.Role.CUSTOMER; // ADMIN can never self-register
    User u = users.save(User.builder().name(r.name()).email(r.email()).passwordHash(encoder.encode(r.password())).role(role).provider("LOCAL").build());
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("token", jwt.issue(u)));
  }
  @PostMapping("/api/auth/login")
  public ResponseEntity<?> login(@Valid @RequestBody LoginReq r) {
    return users.findByEmail(r.email()).filter(u -> u.getPasswordHash() != null && encoder.matches(r.password(), u.getPasswordHash()))
        .<ResponseEntity<?>>map(u -> ResponseEntity.ok(Map.of("token", jwt.issue(u))))
        .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials")));
  }
  @GetMapping("/api/users/me")
  public ResponseEntity<?> me(@RequestHeader("X-User-Id") Long id) {
    return users.findById(id).map(u -> ResponseEntity.ok(Map.of("id", u.getId(), "name", u.getName(), "email", u.getEmail(), "role", u.getRole()))).orElse(ResponseEntity.notFound().build());
  }
  @PutMapping("/api/admin/users/{id}/role") // ADMIN only (enforced at gateway)
  public ResponseEntity<?> setRole(@PathVariable Long id, @RequestParam User.Role role) {
    return users.findById(id).map(u -> { u.setRole(role); users.save(u); return ResponseEntity.ok().build(); }).orElse(ResponseEntity.notFound().build());
  }
}
