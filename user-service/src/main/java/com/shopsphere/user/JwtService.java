package com.shopsphere.user;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Date;
@Service
public class JwtService {
  private final SecretKey key;
  public JwtService(@Value("${jwt.secret}") String secret) { key = Keys.hmacShaKeyFor(secret.getBytes()); }
  public String issue(User u) {
    return Jwts.builder().subject(String.valueOf(u.getId())).claim("email", u.getEmail()).claim("role", u.getRole().name())
        .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 3600_000)).signWith(key).compact();
  }
}
