package com.healthcare.appointment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentLockService {

    private final RedissonClient redissonClient;

    /**
     * Acquires a distributed Redis lock for a specific doctor's time slot.
     * Prevents race conditions and double-booking across multi-pod replicas.
     */
    public <T> T executeWithDoctorSlotLock(String doctorId, String timeSlot, long waitTimeSec, long leaseTimeSec, Supplier<T> action) {
        String lockKey = String.format("lock:doctor:%s:slot:%s", doctorId, timeSlot);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(waitTimeSec, leaseTimeSec, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Failed to acquire distributed lock for doctor {} slot {}", doctorId, timeSlot);
                throw new IllegalStateException("Doctor slot is currently being booked by another patient or is locked.");
            }
            log.info("Redisson RLock acquired for key: {}", lockKey);
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while waiting for distributed lock", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("Redisson RLock released for key: {}", lockKey);
            }
        }
    }
}
