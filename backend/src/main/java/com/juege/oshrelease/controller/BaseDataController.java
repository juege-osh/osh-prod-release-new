package com.juege.oshrelease.controller;

import com.juege.oshrelease.common.ApiResponse;
import com.juege.oshrelease.dto.ComponentDTO;
import com.juege.oshrelease.dto.EnvironmentDTO;
import com.juege.oshrelease.dto.SourceProjectDTO;
import com.juege.oshrelease.service.ComponentService;
import com.juege.oshrelease.service.EnvironmentService;
import com.juege.oshrelease.service.SourceProjectService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BaseDataController {

    private final EnvironmentService environmentService;
    private final ComponentService componentService;
    private final SourceProjectService sourceProjectService;

    public BaseDataController(EnvironmentService environmentService, ComponentService componentService, SourceProjectService sourceProjectService) {
        this.environmentService = environmentService;
        this.componentService = componentService;
        this.sourceProjectService = sourceProjectService;
    }

    @GetMapping("/environments")
    public ApiResponse<List<EnvironmentDTO>> environments() {
        return ApiResponse.ok(environmentService.list());
    }

    @GetMapping("/components")
    public ApiResponse<List<ComponentDTO>> components() {
        return ApiResponse.ok(componentService.list());
    }

    @GetMapping("/source-projects")
    public ApiResponse<List<SourceProjectDTO>> sourceProjects() {
        return ApiResponse.ok(sourceProjectService.list());
    }
}

