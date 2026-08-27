package com.healthcare.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncThreadPoolConfig {

    /**
     * Dedicated isolated thread pool for high-priority Emergency Code-Blue and Critical Vitals alerts.
     * High core pool size and caller-runs policy to prevent queue drops during acute events.
     */
    @Bean(name = "emergencyCodeBlueExecutor")
    public Executor emergencyCodeBlueExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(16);
        executor.setMaxPoolSize(64);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("CodeBlue-Alert-");
        executor.initialize();
        return executor;
    }

    /**
     * Standard thread pool for bulk marketing, appointment reminders, and standard lab reports.
     */
    @Bean(name = "standardNotificationExecutor")
    public Executor standardNotificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("StdNotification-");
        executor.initialize();
        return executor;
    }
}
