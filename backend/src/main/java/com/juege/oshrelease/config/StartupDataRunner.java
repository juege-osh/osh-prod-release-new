package com.juege.oshrelease.config;

import com.juege.oshrelease.service.BootstrapService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupDataRunner implements ApplicationRunner {

    private final BootstrapService bootstrapService;

    public StartupDataRunner(BootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    @Override
    public void run(ApplicationArguments args) {
        bootstrapService.ensureSeedData();
    }
}
