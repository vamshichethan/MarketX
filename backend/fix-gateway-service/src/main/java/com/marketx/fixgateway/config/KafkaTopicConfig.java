package com.marketx.fixgateway.config;

import com.marketx.common.events.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic fixInboundTopic() {
        return topic(KafkaTopics.FIX_INBOUND);
    }

    @Bean
    public NewTopic ordersSubmittedTopic() {
        return topic(KafkaTopics.ORDERS_SUBMITTED);
    }

    @Bean
    public NewTopic ordersCancelRequestedTopic() {
        return topic(KafkaTopics.ORDERS_CANCEL_REQUESTED);
    }

    @Bean
    public NewTopic ordersCancelledTopic() {
        return topic(KafkaTopics.ORDERS_CANCELLED);
    }

    @Bean
    public NewTopic fixExecutionReportsTopic() {
        return topic(KafkaTopics.FIX_EXECUTION_REPORTS);
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
