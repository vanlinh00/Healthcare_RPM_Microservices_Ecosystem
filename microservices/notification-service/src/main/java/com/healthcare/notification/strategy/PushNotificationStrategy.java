package com.healthcare.notification.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("PUSH")
@Slf4j
public class PushNotificationStrategy implements NotificationStrategy {

    @Override
    public String getChannel() {
        return "PUSH";
    }

    @Override
    public boolean sendNotification(String recipient, String title, String body, boolean isEmergency) {
        log.info("[MOBILE PUSH DISPATCHER] DeviceToken: {} | Title: {} | Emergency: {}", recipient, title, isEmergency);
        return true;
    }
}
