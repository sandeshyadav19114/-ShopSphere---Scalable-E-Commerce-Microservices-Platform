package com.shopsphere.order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
public interface OrderRepository extends JpaRepository<Order, Long> {
  List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
  List<Order> findByStatusInAndCreatedAtBefore(Collection<Order.Status> statuses, Instant before);
}
