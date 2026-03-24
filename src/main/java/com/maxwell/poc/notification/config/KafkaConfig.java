package com.maxwell.poc.notification.config;

import com.maxwell.poc.notification.model.NotificationRequest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConfig {

    public static final String TOPIC = "notifications";
    public static final String SHARE_GROUP = "notification-workers";

    /**
     * Le préfixe "share:" est ce qui active le mode Share Group.
     * Sans ce préfixe → consumer group classique.
     * Avec ce préfixe → queue (KIP-932).
     */
    public static final String SHARE_GROUP_ID = "share:" + SHARE_GROUP;

    @Bean
    public ProducerFactory<String, NotificationRequest> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, NotificationRequest> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, NotificationRequest> shareConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.maxwell.poc.notification.model");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, SHARE_GROUP_ID);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Container factory configuré pour le mode Share Consumer.
     * concurrency = 3 → 3 threads consomment en parallèle dans le même share group.
     * Sur un topic à 3 partitions, un consumer group classique serait limité à 3 consumers.
     * Avec un share group, on pourrait monter à 10, 50, 100... sans repartitionner.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationRequest> shareGroupContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, NotificationRequest> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(shareConsumerFactory());
        factory.setConcurrency(3);
        return factory;
    }
}
