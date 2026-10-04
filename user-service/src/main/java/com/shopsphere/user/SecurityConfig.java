package com.shopsphere.user;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean PasswordEncoder encoder() { return new BCryptPasswordEncoder(); }

  /** Gateway already authenticates API calls; this service handles login + OAuth2 social login and issues JWTs. */
  @Bean SecurityFilterChain chain(HttpSecurity http, UserRepository users, JwtService jwt,
                                  @Value("${frontend.url:http://localhost:3000}") String frontend) throws Exception {
    http.csrf(c -> c.disable())
        .authorizeHttpRequests(a -> a.anyRequest().permitAll())
        .oauth2Login(o -> o.successHandler((req, res, auth) -> {
          OAuth2User p = (OAuth2User) auth.getPrincipal();
          String email = p.getAttribute("email");
          User u = users.findByEmail(email).orElseGet(() -> users.save(User.builder()
              .email(email).name(p.getAttribute("name")).role(User.Role.CUSTOMER).provider("GOOGLE").build()));
          res.sendRedirect(frontend + "/oauth-success?token=" + jwt.issue(u));
        }));
    return http.build();
  }
}
