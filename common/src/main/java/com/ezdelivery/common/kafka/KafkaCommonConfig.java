package com.ezdelivery.common.kafka;

import java.util.stream.Stream;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import com.ezdelivery.common.event.Topics;

/**
 * Shared Kafka setup imported by every event-driven service:
 * creates the saga topics (plus a dead-letter topic for each) and configures retries.
 */
@Configuration
public class KafkaCommonConfig {

    private static final int PARTITIONS = 3;

    @Bean
    public KafkaAdmin.NewTopics sagaTopics() {
        NewTopic[] topics = Topics.ALL.stream()
                .flatMap(t -> Stream.of(t, t + Topics.DLT_SUFFIX))
                .map(t -> TopicBuilder.name(t).partitions(PARTITIONS).replicas(1).build())
                .toArray(NewTopic[]::new);
        return new KafkaAdmin.NewTopics(topics);
    }

    /**
     * Retries a failed message 3 more times (1s apart), then parks it on {@code <topic>.DLT}
     * so one bad message cannot block the partition.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> template) {
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), new FixedBackOff(1000L, 3));
    }
}
