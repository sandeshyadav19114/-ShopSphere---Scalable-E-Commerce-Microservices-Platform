package com.shopsphere.payment;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
interface PaymentRepository extends JpaRepository<Payment, Long> { Optional<Payment> findByOrderId(Long id); Optional<Payment> findByRazorpayOrderId(String id); }
@Entity @Table(name = "processed_webhook_events") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
class ProcessedEvent { @Id private String eventId; }
interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {}
