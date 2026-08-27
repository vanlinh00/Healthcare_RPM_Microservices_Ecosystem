package com.healthcare.appointment.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    // In-memory backing buffer for quick transactional outbox event storage
    private final ConcurrentHashMap<String, OutboxEvent> outboxStore = new ConcurrentHashMap<>();

    @Transactional
    public OutboxEvent saveEvent(String aggregateType, String aggregateId, String eventType, String payload) {
        String eventId = UUID.randomUUID().toString();
        OutboxEvent event = OutboxEvent.builder()
                .id(eventId)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .status("PENDING")
                .retryCount(0)
                .createdAt(ZonedDateTime.now())
                .build();

        outboxStore.put(eventId, event);
        log.info("Transactional Outbox: Event persisted for aggregate {}:{}", aggregateType, aggregateId);
        return event;
    }

    public void publishPendingEvents() {
        outboxStore.values().stream()
                .filter(e -> "PENDING".equals(e.getStatus()))
                .forEach(event -> {
                    try {
                        String topic = "healthcare." + event.getAggregateType().toLowerCase() + ".events";
                        kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload())
                                .whenComplete((result, ex) -> {
                                    if (ex == null) {
                                        event.setStatus("SENT");
                                        event.setProcessedAt(ZonedDateTime.now());
                                        log.info("Kafka Outbox Publisher: Successfully published event {} to topic {}", event.getId(), topic);
                                    } else {
                                        event.setRetryCount(event.getRetryCount() + 1);
                                        if (event.getRetryCount() > 3) {
                                            event.setStatus("FAILED");
                                        }
                                        log.error("Kafka Outbox Publisher: Failed to publish event {}", event.getId(), ex);
                                    }
                                });
                    } catch (Exception e) {
                        log.error("Error pushing outbox event to Kafka", e);
                    }
                });
    }

    public ConcurrentHashMap<String, OutboxEvent> getOutboxStore() {
        return outboxStore;
    }
}
