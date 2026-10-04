package com.shopsphere.notification;
import com.shopsphere.common.events.Events.OrderEvent;
import com.shopsphere.common.events.Topics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class NotificationServiceApplication { public static void main(String[] a){ SpringApplication.run(NotificationServiceApplication.class,a);} }

/** Swap the log lines for JavaMailSender / AWS SES / SNS to send real email/SMS. */
@Component @Slf4j
class OrderNotificationListener {
  @KafkaListener(topics = Topics.ORDER_EVENTS)
  void on(OrderEvent e) {
    String msg = switch (e.status()) {
      case "CONFIRMED" -> "Your order #%d is confirmed (Rs %s)";
      case "SHIPPED" -> "Your order #%d has shipped";
      case "OUT_FOR_DELIVERY" -> "Your order #%d is out for delivery";
      case "DELIVERED" -> "Your order #%d was delivered";
      case "CANCELLED" -> "Your order #%d was cancelled";
      default -> null;
    };
    if (msg != null) log.info("EMAIL to {}: {}", e.email(), String.format(msg, e.orderId(), e.total()));
  }
}
