package com.shopsphere.order;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
@FeignClient(name = "PRODUCT-SERVICE")
public interface ProductClient {
  record ProductDto(Long id, String name, BigDecimal price) {}
  @GetMapping("/api/products/{id}") ProductDto get(@PathVariable("id") Long id);
}
