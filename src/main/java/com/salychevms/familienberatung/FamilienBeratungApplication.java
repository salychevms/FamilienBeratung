package com.salychevms.familienberatung;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FamilienBeratungApplication {

    public static void main(String[] args) {
        SpringApplication.run(FamilienBeratungApplication.class, args);
    }

}
