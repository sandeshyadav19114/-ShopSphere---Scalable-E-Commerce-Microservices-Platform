package com.shopsphere.product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
public interface ProductRepository extends JpaRepository<Product, Long> {
  /** Keyset (cursor) pagination: O(log n) seek on PK instead of OFFSET scans. */
  @Query("""
    select p from Product p where p.id > :cursor
      and (:category is null or p.category = :category)
      and (:brand is null or p.brand = :brand)
      and (:min is null or p.price >= :min) and (:max is null or p.price <= :max)
      and (:q is null or p.name like concat(:q, '%'))
    order by p.id""")
  List<Product> page(@Param("cursor") long cursor, @Param("category") String category, @Param("brand") String brand,
                     @Param("min") BigDecimal min, @Param("max") BigDecimal max, @Param("q") String q, Pageable pageable);
}
