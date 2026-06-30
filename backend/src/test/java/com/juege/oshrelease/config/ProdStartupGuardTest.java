package com.juege.oshrelease.config;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProdStartupGuardTest {

    @Test
    void shouldRejectDefaultProductionSecrets() {
        MockEnvironment environment = baseEnvironment();
        environment.setProperty("app.jwt.secret", "osh-release-governance-secret-key-change-me");

        ProdStartupGuard guard = new ProdStartupGuard(environment);

        assertThrows(IllegalStateException.class, () -> guard.run(null));
    }

    @Test
    void shouldRequireAllSeedPasswords() {
        MockEnvironment environment = baseEnvironment();
        environment.setProperty("app.seed-users.ops-password", "");

        ProdStartupGuard guard = new ProdStartupGuard(environment);

        assertThrows(IllegalStateException.class, () -> guard.run(null));
    }

    @Test
    void shouldRejectExamplePlaceholderSecrets() {
        MockEnvironment environment = baseEnvironment();
        environment.setProperty("spring.datasource.password", "replace-with-strong-database-password");

        ProdStartupGuard guard = new ProdStartupGuard(environment);

        assertThrows(IllegalStateException.class, () -> guard.run(null));
    }

    @Test
    void shouldAllowStrongProductionConfig() {
        ProdStartupGuard guard = new ProdStartupGuard(baseEnvironment());

        assertDoesNotThrow(() -> guard.run(null));
    }

    private MockEnvironment baseEnvironment() {
        Map<String, String> values = new HashMap<String, String>();
        values.put("app.jwt.secret", "prod-jwt-secret-with-more-than-32-chars");
        values.put("spring.datasource.password", "prod-db-password-strong");
        values.put("app.seed-users.juege-password", "owner-password-strong");
        values.put("app.seed-users.reviewer-a-password", "reviewer-a-password-strong");
        values.put("app.seed-users.reviewer-b-password", "reviewer-b-password-strong");
        values.put("app.seed-users.ops-password", "ops-password-strong");

        MockEnvironment environment = new MockEnvironment();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            environment.setProperty(entry.getKey(), entry.getValue());
        }
        return environment;
    }
}
