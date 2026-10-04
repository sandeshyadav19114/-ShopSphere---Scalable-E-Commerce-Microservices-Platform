package com.shopsphere.payment;
import com.razorpay.RazorpayClient;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.*;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class RazorpayConfig {
  @Bean RazorpayClient razorpay(@Value("${razorpay.key-id}") String id, @Value("${razorpay.key-secret}") String secret) throws Exception { return new RazorpayClient(id, secret); }

  /** Poison / repeatedly-failing payment messages: 3 retries, then parked on "<topic>.DLT" for inspection & replay. */
  @Bean DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> t) {
    return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(t), new FixedBackOff(1000L, 3));
  }
}
