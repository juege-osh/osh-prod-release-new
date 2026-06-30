package com.juege.oshrelease.service.impl;

import com.juege.oshrelease.model.AppUser;
import com.juege.oshrelease.repo.AppUserRepository;
import com.juege.oshrelease.repo.ComponentRepository;
import com.juege.oshrelease.repo.EnvironmentRepository;
import com.juege.oshrelease.repo.ReleaseChangeItemRepository;
import com.juege.oshrelease.repo.ReleaseChangeRepository;
import com.juege.oshrelease.repo.ReleaseNodeRepository;
import com.juege.oshrelease.repo.ReleaseOperationRecordRepository;
import com.juege.oshrelease.repo.ReviewerTestEvidenceRepository;
import com.juege.oshrelease.repo.ReviewRecordRepository;
import com.juege.oshrelease.repo.SourceProjectRepository;
import com.juege.oshrelease.repo.TestReportRepository;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class BootstrapServiceImplSeedUserTest {

    @Test
    void seedUsersShouldReadPasswordsFromConfiguration() throws Exception {
        AppUserRepository appUserRepository = Mockito.mock(AppUserRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        Map<String, AppUser> savedUsers = new LinkedHashMap<String, AppUser>();
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.seed-users.juege-password", "owner-secret")
                .withProperty("app.seed-users.reviewer-a-password", "reviewer-a-secret")
                .withProperty("app.seed-users.reviewer-b-password", "reviewer-b-secret")
                .withProperty("app.seed-users.ops-password", "ops-secret");

        when(appUserRepository.findByUsername(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenAnswer(invocation -> "hashed-" + invocation.getArgument(0));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            savedUsers.put(user.getUsername(), user);
            return user;
        });

        BootstrapServiceImpl service = service(appUserRepository, passwordEncoder, environment);
        invokeSeedUsers(service);

        assertEquals("hashed-owner-secret", savedUsers.get("juege").getPasswordHash());
        assertEquals("hashed-reviewer-a-secret", savedUsers.get("reviewer_a").getPasswordHash());
        assertEquals("hashed-reviewer-b-secret", savedUsers.get("reviewer_b").getPasswordHash());
        assertEquals("hashed-ops-secret", savedUsers.get("ops").getPasswordHash());
    }

    @Test
    void seedUsersShouldFailWhenNewUserPasswordIsMissing() throws Exception {
        AppUserRepository appUserRepository = Mockito.mock(AppUserRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.seed-users.reviewer-a-password", "reviewer-a-secret")
                .withProperty("app.seed-users.reviewer-b-password", "reviewer-b-secret")
                .withProperty("app.seed-users.ops-password", "ops-secret");

        when(appUserRepository.findByUsername(any())).thenReturn(Optional.empty());

        BootstrapServiceImpl service = service(appUserRepository, passwordEncoder, environment);

        assertThrows(IllegalStateException.class, () -> invokeSeedUsers(service));
    }

    private BootstrapServiceImpl service(AppUserRepository appUserRepository,
                                         PasswordEncoder passwordEncoder,
                                         MockEnvironment environment) {
        return new BootstrapServiceImpl(
                appUserRepository,
                Mockito.mock(EnvironmentRepository.class),
                Mockito.mock(ComponentRepository.class),
                Mockito.mock(SourceProjectRepository.class),
                Mockito.mock(ReleaseChangeRepository.class),
                Mockito.mock(ReleaseNodeRepository.class),
                Mockito.mock(ReleaseChangeItemRepository.class),
                Mockito.mock(ReviewRecordRepository.class),
                Mockito.mock(ReviewerTestEvidenceRepository.class),
                Mockito.mock(ReleaseOperationRecordRepository.class),
                Mockito.mock(TestReportRepository.class),
                passwordEncoder,
                environment
        );
    }

    private void invokeSeedUsers(BootstrapServiceImpl service) throws Exception {
        Method method = BootstrapServiceImpl.class.getDeclaredMethod("seedUsers");
        method.setAccessible(true);
        try {
            method.invoke(service);
        } catch (java.lang.reflect.InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw exception;
        }
    }
}
