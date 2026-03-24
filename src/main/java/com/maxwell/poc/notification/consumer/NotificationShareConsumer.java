package com.maxwell.poc.notification.consumer;

import com.maxwell.poc.notification.config.KafkaConfig;
import com.maxwell.poc.notification.model.NotificationRequest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Share Consumer pour les notifications.
 *
 * Différences clés avec un @KafkaListener classique :
 * - Le groupId "share:notification-workers" est défini dans KafkaConfig (via la ConsumerFactory)
 * - Le préfixe "share:" active le mode Share Group
 * - Chaque message est acquitté individuellement (ACCEPT / RELEASE / REJECT)
 * - Plusieurs consumers peuvent traiter des messages de la MÊME partition en parallèle
 * - Pas de garantie d'ordre → parfait pour des tâches indépendantes
 */
@Component
public class NotificationShareConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationShareConsumer.class);

    @KafkaListener(
            topics = KafkaConfig.TOPIC,
            containerFactory = "shareGroupContainerFactory"
    )
    public void processNotification(ConsumerRecord<String, NotificationRequest> record,
                                    Acknowledgment acknowledgment) {

        NotificationRequest notification = record.value();
        String threadName = Thread.currentThread().getName();

        log.info("[{}] 📨 Received: type={}, recipient={}, subject='{}'",
                threadName, notification.type(), notification.recipient(), notification.subject());

        try {
            // Simule un traitement (envoi email, push, SMS...)
            simulateProcessing(notification);

            // ✅ ACCEPT — message traité avec succès
            acknowledgment.acknowledge();
            log.info("[{}] ✅ Acknowledged: id={}", threadName, notification.id());

        } catch (RetryableException e) {
            // 🔄 RELEASE — remettre le message dans la queue pour retry
            // Un autre worker (ou le même) le reprendra
            log.warn("[{}] 🔄 Released for retry: id={}, reason='{}'",
                    threadName, notification.id(), e.getMessage());
            // acknowledgment.release();  // ← décommenter quand l'API est disponible

        } catch (Exception e) {
            // ❌ REJECT — échec permanent, ne pas retenter
            log.error("[{}] ❌ Rejected: id={}, error='{}'",
                    threadName, notification.id(), e.getMessage());
            // acknowledgment.reject();   // ← décommenter quand l'API est disponible
        }
    }

    private void simulateProcessing(NotificationRequest notification) {
        try {
            // Simule un temps de traitement variable (200-800ms)
            long processingTime = 200 + (long) (Math.random() * 600);
            Thread.sleep(processingTime);

            // Simule un échec 1 fois sur 10
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