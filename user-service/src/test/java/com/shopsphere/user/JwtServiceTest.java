package com.shopsphere.user;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
  static final String SECRET = "unit-test-secret-unit-test-secret-1234";
  @Test void issuedTokenCarriesIdentityAndRole() {
    User u = User.builder().id(42L).email("a@b.com").role(User.Role.SELLER).build();
    var claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes())).build().parseSignedClaims(new JwtService(SECRET).issue(u)).getPayload();
    assertEquals("42", claims.getSubject()); assertEquals("SELLER", claims.get("role")); assertEquals("a@b.com", claims.get("email"));
  }
  @Test void tokenSignedWithAnotherKeyIsRejected() {
    String token = new JwtService("another-secret-another-secret-another-1").issue(User.builder().id(1L).email("x@y.z").role(User.Role.CUSTOMER).build());
    assertThrows(Exception.class, () -> Jwts.parser().verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes())).build().parseSignedClaims(token));
  }
}
