package com.healthcare.dispatch.saga;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class CareDispatchSagaParticipant {

    private final ConcurrentHashMap<String, String> reservedDispatches = new ConcurrentHashMap<>();

    public boolean reserveCareResource(String sagaId, String resourceId) {
        log.info("Care Dispatch Saga: Reserving responder resource {} for Saga {}", resourceId, sagaId);
        reservedDispatches.put(sagaId, resourceId);
        return true;
    }

    public void rollbackReservation(String sagaId) {
        String resourceId = reservedDispatches.remove(sagaId);
        if (resourceId != null) {
            log.warn("Care Dispatch Saga: Compensated & released responder resource {} for Saga {}", resourceId, sagaId);
        }
    }
}
