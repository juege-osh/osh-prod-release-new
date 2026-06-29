package com.juege.oshrelease.service.impl;

import com.juege.oshrelease.common.BusinessException;
import com.juege.oshrelease.common.UnauthorizedException;
import com.juege.oshrelease.config.JwtService;
import com.juege.oshrelease.config.RequestContext;
import com.juege.oshrelease.dto.AuthLoginRequest;
import com.juege.oshrelease.dto.AuthLoginResponse;
import com.juege.oshrelease.dto.CurrentUserDTO;
import com.juege.oshrelease.model.AppUser;
import com.juege.oshrelease.repo.AppUserRepository;
import com.juege.oshrelease.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public AuthLoginResponse login(AuthLoginRequest request) {
        if (request == null || isBlank(request.username) || isBlank(request.password)) {
            throw new BusinessException("请输入用户名和密码");
        }
        AppUser user = appUserRepository.findByUsername(request.username.trim())
                .orElseThrow(() -> new UnauthorizedException("用户名或密码不正确"));
        if (!user.isEnabled() || !passwordEncoder.matches(request.password, user.getPasswordHash())) {
            throw new UnauthorizedException("用户名或密码不正确");
        }
        AuthLoginResponse response = new AuthLoginResponse();
        response.userId = user.getId();
        response.username = user.getUsername();
        response.displayName = user.getDisplayName();
        response.role = user.getRole();
        response.token = jwtService.createToken(user);
        return response;
    }

    @Override
    public CurrentUserDTO currentUser() {
        AppUser user = RequestContext.getCurrentUser();
        if (user == null) {
            throw new UnauthorizedException("未登录");
        }
        CurrentUserDTO dto = new CurrentUserDTO();
        dto.userId = user.getId();
        dto.username = user.getUsername();
        dto.displayName = user.getDisplayName();
        dto.role = user.getRole();
        return dto;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
