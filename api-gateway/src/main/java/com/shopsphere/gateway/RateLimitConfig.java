package com.shopsphere.gateway;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.*;
import reactor.core.publisher.Mono;
@Configuration
public class RateLimitConfig {
  /** Rate-limit per bearer token when present, otherwise per client IP. */
  @Bean KeyResolver userKeyResolver() {
    return ex -> {
      String auth = ex.getRequest().getHeaders().getFirst("Authorization");
      if (auth != null) return Mono.just(Integer.toHexString(auth.hashCode()));
      var addr = ex.getRequest().getRemoteAddress();
      return Mono.just(addr == null ? "anon" : addr.getAddress().getHostAddress());
    };
  }
}
