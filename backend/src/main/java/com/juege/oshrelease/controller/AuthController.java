package com.juege.oshrelease.controller;

import com.juege.oshrelease.common.ApiResponse;
import com.juege.oshrelease.dto.AuthLoginRequest;
import com.juege.oshrelease.dto.AuthLoginResponse;
import com.juege.oshrelease.dto.CurrentUserDTO;
import com.juege.oshrelease.service.AuthService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthLoginResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserDTO> me() {
        return ApiResponse.ok(authService.currentUser());
    }
}

