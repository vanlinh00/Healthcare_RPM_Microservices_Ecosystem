package com.healthcare.dispatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CareDispatchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareDispatchServiceApplication.class, args);
    }
}
