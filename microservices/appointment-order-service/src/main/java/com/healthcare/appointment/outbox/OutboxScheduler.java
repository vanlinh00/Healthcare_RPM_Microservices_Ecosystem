package com.healthcare.appointment.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduler {

    private final OutboxPublisherService outboxPublisherService;

    /**
     * Polls outbox every 2 seconds to dispatch pending events to Apache Kafka broker.
     */
    @Scheduled(fixedDelay = 2000)
    public void processOutboxEvents() {
        outboxPublisherService.publishPendingEvents();
    }
}
