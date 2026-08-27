package com.healthcare.notification.consumer;

import com.healthcare.notification.strategy.NotificationStrategy;
import com.healthcare.notification.strategy.NotificationStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaNotificationEventConsumer {

    private final NotificationStrategyFactory strategyFactory;

    @KafkaListener(topics = "healthcare.appointment.events", groupId = "notification-service-group")
    public void handleAppointmentEvent(String eventPayload) {
        log.info("Kafka Notification Consumer received appointment event: {}", eventPayload);

        // Dispatch email notification via strategy
        NotificationStrategy emailStrategy = strategyFactory.getStrategy("EMAIL");
        emailStrategy.sendNotification("patient@healthcare.internal", "Appointment Confirmation", eventPayload, false);

        // Dispatch SMS reminder
        NotificationStrategy smsStrategy = strategyFactory.getStrategy("SMS");
        smsStrategy.sendNotification("+1-555-0199", "Healthcare Reminder", "Your consultation has been booked.", false);
    }

    @KafkaListener(topics = "healthcare.emergency.events", groupId = "emergency-alert-group")
    @Async("emergencyCodeBlueExecutor")
    public void handleEmergencyAlert(String alertPayload) {
        log.warn("CRITICAL EMERGENCY CODE BLUE ALERT RECEIVED: {}", alertPayload);

        // High priority push & SMS to on-call physician & nearest ER team
        NotificationStrategy pushStrategy = strategyFactory.getStrategy("PUSH");
        pushStrategy.sendNotification("DEVICE_TOKEN_ICU_TEAM_1", "🚨 CODE BLUE ALERT", alertPayload, true);

        NotificationStrategy smsStrategy = strategyFactory.getStrategy("SMS");
        smsStrategy.sendNotification("+1-911-DISPATCH", "🚨 CODE BLUE", alertPayload, true);
    }
}
