package com.juege.oshrelease.config;

import com.juege.oshrelease.model.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final AppProperties appProperties;

    public JwtService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String createToken(AppUser user) {
        long now = System.currentTimeMillis();
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("uid", user.getId());
        claims.put("username", user.getUsername());
        claims.put("displayName", user.getDisplayName());
        claims.put("role", user.getRole());
        return Jwts.builder()
                .setClaims(claims)
                .setIssuer(appProperties.getJwt().getIssuer())
                .setSubject(user.getUsername())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + appProperties.getJwt().getTtlSeconds() * 1000L))
                .signWith(SignatureAlgorithm.HS256, appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8))
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8))
                .parseClaimsJws(token)
                .getBody();
    }
}

