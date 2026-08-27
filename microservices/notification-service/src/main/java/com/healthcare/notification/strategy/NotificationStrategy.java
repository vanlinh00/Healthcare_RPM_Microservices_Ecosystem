package com.healthcare.notification.strategy;

public interface NotificationStrategy {
    String getChannel();
    boolean sendNotification(String recipient, String title, String body, boolean isEmergency);
}
