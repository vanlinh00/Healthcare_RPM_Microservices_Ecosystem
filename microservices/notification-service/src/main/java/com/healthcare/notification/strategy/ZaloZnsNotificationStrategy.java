package com.healthcare.notification.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("ZALO_ZNS")
@Slf4j
public class ZaloZnsNotificationStrategy implements NotificationStrategy {

    @Override
    public String getChannel() {
        return "ZALO_ZNS";
    }

    @Override
    public boolean sendNotification(String recipient, String title, String body, boolean isEmergency) {
        log.info("[ZALO ZNS OTT DISPATCHER] Phone: {} | Template Title: {} | Emergency: {}", recipient, title, isEmergency);
        return true;
    }
}
