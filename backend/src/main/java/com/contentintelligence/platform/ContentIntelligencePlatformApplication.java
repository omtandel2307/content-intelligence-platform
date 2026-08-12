package com.contentintelligence.platform;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableCaching
@SpringBootApplication
public class ContentIntelligencePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentIntelligencePlatformApplication.class, args);
    }
}
