package com.maxwell.poc.notification.model;

/**
 * Notification à envoyer — chaque instance est une tâche indépendante.
 * Pas d'ordre requis entre les notifications → use case parfait pour Share Groups.
 */
public record NotificationRequest(
        String id,
        String recipient,
        NotificationType type,
        String subject,
        String body
) {
    public enum NotificationType {
        EMAIL, PUSH, SMS
    }
}
