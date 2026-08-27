package com.healthcare.notification.strategy;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NotificationStrategyFactory {

    private final Map<String, NotificationStrategy> strategyMap;

    public NotificationStrategyFactory(Map<String, NotificationStrategy> strategyMap) {
        this.strategyMap = strategyMap;
    }

    public NotificationStrategy getStrategy(String channel) {
        NotificationStrategy strategy = strategyMap.get(channel.toUpperCase());
        if (strategy == null) {
            return strategyMap.get("EMAIL");
        }
        return strategy;
    }
}
