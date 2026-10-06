package com.yuna.yunashop.order.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Chaque service déclare les topics qu'il produit. */
@Configuration
class KafkaTopicConfig {

    @Bean
    NewTopic orderCreatedTopic() {
        return TopicBuilder.name(Topics.ORDER_CREATED).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic orderCancelledTopic() {
        return TopicBuilder.name(Topics.ORDER_CANCELLED).partitions(3).replicas(1).build();
    }
}
