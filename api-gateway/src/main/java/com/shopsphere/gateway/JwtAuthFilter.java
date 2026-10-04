package com.shopsphere.gateway;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.*;
import org.springframework.core.Ordered;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import javax.crypto.SecretKey;
import java.util.List;

/** Validates JWT, enforces coarse RBAC (Customer/Seller/Admin) and injects trusted identity headers downstream. */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {
  private static final List<String> PUBLIC = List.of("/api/auth/", "/oauth2/", "/login/oauth2/", "/api/payments/webhook", "/actuator/health");
  private final SecretKey key;
  public JwtAuthFilter(@Value("${jwt.secret}") String secret) { this.key = Keys.hmacShaKeyFor(secret.getBytes()); }

  @Override public Mono<Void> filter(ServerWebExchange ex, GatewayFilterChain chain) {
    ServerHttpRequest req = ex.getRequest();
    String path = req.getURI().getPath();
    boolean read = req.getMethod() == HttpMethod.GET;
    if (PUBLIC.stream().anyMatch(path::startsWith) || (read && path.startsWith("/api/products"))) return chain.filter(ex);
    String h = req.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (h == null || !h.startsWith("Bearer ")) return reject(ex, HttpStatus.UNAUTHORIZED);
    try {
      Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(h.substring(7)).getPayload();
      String role = c.get("role", String.class);
      if (path.startsWith("/api/admin") && !"ADMIN".equals(role)) return reject(ex, HttpStatus.FORBIDDEN);
      boolean seller = "SELLER".equals(role) || "ADMIN".equals(role);
      if ((path.startsWith("/api/products") || path.startsWith("/api/inventory")) && !seller) return reject(ex, HttpStatus.FORBIDDEN);
      ServerHttpRequest out = req.mutate().headers(hd -> { hd.remove("X-User-Id"); hd.remove("X-User-Role"); hd.remove("X-User-Email"); })
          .header("X-User-Id", c.getSubject()).header("X-User-Role", role).header("X-User-Email", c.get("email", String.class)).build();
      return chain.filter(ex.mutate().request(out).build());
    } catch (JwtException | IllegalArgumentException e) { return reject(ex, HttpStatus.UNAUTHORIZED); }
  }
  private Mono<Void> reject(ServerWebExchange ex, HttpStatus s) { ex.getResponse().setStatusCode(s); return ex.getResponse().setComplete(); }
  @Override public int getOrder() { return -1; }
}
