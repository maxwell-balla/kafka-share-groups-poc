package com.maxwell.poc.notification.producer;

import com.maxwell.poc.notification.config.KafkaConfig;
import com.maxwell.poc.notification.model.NotificationRequest;
import com.maxwell.poc.notification.model.NotificationRequest.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationProducer {

    private static final Logger log = LoggerFactory.getLogger(NotificationProducer.class);

    private final KafkaTemplate<String, NotificationRequest> kafkaTemplate;

    public NotificationProducer(KafkaTemplate<String, NotificationRequest> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping
    public NotificationRequest send(@RequestBody NotificationRequest request) {
        kafkaTemplate.send(KafkaConfig.TOPIC, request.id(), request);
        log.info("📤 Sent notification: id={}, type={}, to='{}'",
                request.id(), request.type(), request.recipient());
        return request;
    }

    @PostMapping("/burst")
    public List<String> sendBurst(@RequestParam(defaultValue = "50") int count) {
        List<String> ids = new ArrayList<>();
        NotificationType[] types = NotificationType.values();

        for (int i = 0; i < count; i++) {
            String id = UUID.randomUUID().toString().substring(0, 8);
            NotificationType type = types[i % types.length];

            NotificationRequest request = new NotificationRequest(
                    id,
                    "user-" + (i % 10) + "@example.com",
                    type,
                    "Notification #" + i,
                    "This is test notification number " + i
            );

            kafkaTemplate.send(KafkaConfig.TOPIC, id, request);
            ids.add(id);
        }

        log.info("📤 Burst sent: {} notifications", count);
        return ids;
    }
}
