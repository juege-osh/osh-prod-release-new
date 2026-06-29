package com.juege.oshrelease;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OshReleaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(OshReleaseApplication.class, args);
    }
}

