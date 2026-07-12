package com.mtdsubmitter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MtdSubmitterApplication {
    public static void main(String[] args) {
        SpringApplication.run(MtdSubmitterApplication.class, args);
    }
}
