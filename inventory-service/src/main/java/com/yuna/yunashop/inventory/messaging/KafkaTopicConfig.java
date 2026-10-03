package com.yuna.yunashop.inventory.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Chaque service déclare les topics qu'il produit. */
@Configuration
class KafkaTopicConfig {

    @Bean
    NewTopic inventoryReservedTopic() {
        return TopicBuilder.name(Topics.INVENTORY_RESERVED).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic inventoryFailedTopic() {
        return TopicBuilder.name(Topics.INVENTORY_FAILED).partitions(3).replicas(1).build();
    }
}
