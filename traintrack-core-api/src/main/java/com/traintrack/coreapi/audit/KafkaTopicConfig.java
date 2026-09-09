package com.traintrack.coreapi.audit;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * 3 partitions rather than the broker's single-partition auto-create default,
 * so the orgId partition key actually does something demonstrable — with one
 * partition, "events for an org stay ordered" would be trivially true
 * regardless of the key.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic auditEventsTopic() {
        return TopicBuilder.name("audit.events").partitions(3).replicas(1).build();
    }
}
