package com.juege.oshrelease.config;

import com.juege.oshrelease.common.UnauthorizedException;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestAuthInterceptor implements HandlerInterceptor {

    private static final List<String> PUBLIC_PREFIXES = Arrays.asList(
            "/api/auth/login",
            "/api/system",
            "/actuator",
            "/"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return true;
            }
        }
        if (RequestContext.getCurrentUser() == null) {
            throw new UnauthorizedException("未登录");
        }
        return true;
    }
}

