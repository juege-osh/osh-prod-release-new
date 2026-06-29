package com.juege.oshrelease.controller;

import com.juege.oshrelease.common.ApiResponse;
import com.juege.oshrelease.service.BootstrapService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bootstrap")
public class BootstrapController {

    private final BootstrapService bootstrapService;

    public BootstrapController(BootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    @PostMapping("/seed")
    public ApiResponse<String> seed() {
        bootstrapService.ensureSeedData();
        return ApiResponse.ok("seeded");
    }
}

