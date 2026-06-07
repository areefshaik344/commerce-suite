package com.commercesuite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class CommerceSuiteApplication {
    public static void main(String[] args) {
        SpringApplication.run(CommerceSuiteApplication.class, args);
    }
}
