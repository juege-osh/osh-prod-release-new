package com.juege.oshrelease.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProdStartupGuard implements ApplicationRunner {

    private static final String DEFAULT_DB_PASSWORD = "osh_release";
    private static final String DEFAULT_JWT_SECRET = "osh-release-governance-secret-key-change-me";
    private static final List<String> REQUIRED_SEED_KEYS = Arrays.asList(
            "app.seed-users.juege-password",
            "app.seed-users.reviewer-a-password",
            "app.seed-users.reviewer-b-password",
            "app.seed-users.ops-password"
    );

    private final Environment environment;

    public ProdStartupGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String jwtSecret = environment.getProperty("app.jwt.secret", "");
        if (isWeakSecret(jwtSecret) || DEFAULT_JWT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException("生产环境必须设置 APP_JWT_SECRET，不能使用默认 JWT 密钥。");
        }
        String dbPassword = environment.getProperty("spring.datasource.password", "");
        if (isWeakSecret(dbPassword) || DEFAULT_DB_PASSWORD.equals(dbPassword)) {
            throw new IllegalStateException("生产环境必须设置 OSH_DB_PASSWORD，不能使用默认数据库密码。");
        }
        for (String key : REQUIRED_SEED_KEYS) {
            if (isWeakSecret(environment.getProperty(key))) {
                throw new IllegalStateException("生产环境必须设置种子用户密码：" + key);
            }
        }
    }

    private boolean isWeakSecret(String value) {
        if (isBlank(value)) {
            return true;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.startsWith("change-me")
                || normalized.startsWith("replace-with")
                || normalized.contains("please-change");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
