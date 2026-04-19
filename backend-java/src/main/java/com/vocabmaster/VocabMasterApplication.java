package com.vocabmaster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class VocabMasterApplication {

    public static void main(String[] args) {
        SpringApplication.run(VocabMasterApplication.class, args);
    }
}
