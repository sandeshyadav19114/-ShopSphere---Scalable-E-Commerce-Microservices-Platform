package com.shopsphere.order;
import com.shopsphere.common.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import java.util.stream.Stream;

/** Declares every topic with 3 partitions (keyed by orderId => per-order ordering, parallelism across orders) + DLTs for payment topics. */
@Configuration
class KafkaTopicsConfig {
  @Bean KafkaAdmin.NewTopics topics(@Value("${kafka.replicas:1}") short replicas) {
    return new KafkaAdmin.NewTopics(Stream.of(Topics.INVENTORY_RESERVE_CMD, Topics.INVENTORY_RELEASE_CMD, Topics.INVENTORY_RESERVED, Topics.INVENTORY_FAILED,
        Topics.PAYMENT_REQUEST_CMD, Topics.PAYMENT_REFUND_CMD, Topics.PAYMENT_RESULT, Topics.ORDER_EVENTS,
        Topics.PAYMENT_REQUEST_CMD + ".DLT", Topics.PAYMENT_REFUND_CMD + ".DLT")
        .map(n -> TopicBuilder.name(n).partitions(3).replicas(replicas).build()).toArray(NewTopic[]::new));
  }
}
