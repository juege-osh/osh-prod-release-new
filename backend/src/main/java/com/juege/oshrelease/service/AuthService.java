package com.juege.oshrelease.service;

import com.juege.oshrelease.dto.AuthLoginRequest;
import com.juege.oshrelease.dto.AuthLoginResponse;
import com.juege.oshrelease.dto.CurrentUserDTO;

public interface AuthService {
    AuthLoginResponse login(AuthLoginRequest request);
    CurrentUserDTO currentUser();
}

