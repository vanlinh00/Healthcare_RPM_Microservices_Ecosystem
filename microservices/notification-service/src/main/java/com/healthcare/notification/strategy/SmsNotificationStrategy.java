package com.healthcare.notification.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("SMS")
@Slf4j
public class SmsNotificationStrategy implements NotificationStrategy {

    @Override
    public String getChannel() {
        return "SMS";
    }

    @Override
    public boolean sendNotification(String recipient, String title, String body, boolean isEmergency) {
        log.info("[SMS DISPATCHER] To: {} | Msg: {} | Priority: {}", recipient, body, isEmergency ? "HIGH" : "NORMAL");
        return true;
    }
}
