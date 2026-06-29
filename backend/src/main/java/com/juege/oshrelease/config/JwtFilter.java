package com.juege.oshrelease.config;

import com.juege.oshrelease.common.UnauthorizedException;
import com.juege.oshrelease.model.AppUser;
import com.juege.oshrelease.repo.AppUserRepository;
import io.jsonwebtoken.Claims;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;

    public JwtFilter(JwtService jwtService, AppUserRepository appUserRepository) {
        this.jwtService = jwtService;
        this.appUserRepository = appUserRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String path = request.getRequestURI();
            if (path != null && (path.startsWith("/api/auth/login") || path.startsWith("/actuator") || path.startsWith("/error"))) {
                filterChain.doFilter(request, response);
                return;
            }
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7).trim();
                Claims claims = jwtService.parseToken(token);
                Long userId = ((Number) claims.get("uid")).longValue();
                Optional<AppUser> optionalUser = appUserRepository.findById(userId);
                if (optionalUser.isPresent()) {
                    RequestContext.setCurrentUser(optionalUser.get());
                }
            }
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            throw new UnauthorizedException("登录已失效，请重新登录");
        } finally {
            RequestContext.clear();
        }
    }
}

