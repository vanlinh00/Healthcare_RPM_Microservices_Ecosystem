package com.healthcare.notification.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("EMAIL")
@Slf4j
public class EmailNotificationStrategy implements NotificationStrategy {

    @Override
    public String getChannel() {
        return "EMAIL";
    }

    @Override
    public boolean sendNotification(String recipient, String title, String body, boolean isEmergency) {
        log.info("[EMAIL DISPATCHER] To: {} | Subject: {} | Emergency: {}", recipient, title, isEmergency);
        return true;
    }
}
