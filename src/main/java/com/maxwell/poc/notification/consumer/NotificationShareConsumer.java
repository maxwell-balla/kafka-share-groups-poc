package com.maxwell.poc.notification.consumer;

import com.maxwell.poc.notification.config.KafkaConfig;
import com.maxwell.poc.notification.model.NotificationRequest;
import org.apache.kafka.clients.consumer.AcknowledgeType;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.ShareAcknowledgment;
import org.springframework.stereotype.Component;


@Component
public class NotificationShareConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationShareConsumer.class);

    @KafkaListener(
            topics = KafkaConfig.TOPIC,
            containerFactory = "shareGroupContainerFactory"
    )
    public void processNotification(ConsumerRecord<String, NotificationRequest> record,
                                    ShareAcknowledgment acknowledgment) {

        NotificationRequest notification = record.value();
        String threadName = Thread.currentThread().getName();

        log.info("[{}] 📨 Received: type={}, recipient={}, subject='{}'",
                threadName, notification.type(), notification.recipient(), notification.subject());

        try {
            simulateProcessing(notification);
            acknowledgment.acknowledge();
            log.info("[{}] ✅ Accepted: id={}", threadName, notification.id());

        } catch (RetryableException e) {
            acknowledgment.release();
            log.warn("[{}] 🔄 Released for retry: id={}, reason='{}'",
                    threadName, notification.id(), e.getMessage());

        } catch (Exception e) {
            acknowledgment.reject();
            log.error("[{}] ❌ Rejected: id={}, error='{}'",
                    threadName, notification.id(), e.getMessage());
        }
    }

    private void simulateProcessing(NotificationRequest notification) {
        try {
            long processingTime = 200 + (long) (Math.random() * 600);
            Thread.sleep(processingTime);

            if (Math.random() < 0.1) {
                throw new RetryableException("Temporary failure sending " + notification.type());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static class RetryableException extends RuntimeException {
        RetryableException(String message) { super(message); }
    }
}