package com.github.drnow4u.fakeproducer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FakeProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FakeProducerApplication.class, args);
    }

}
