package com.juege.oshrelease.repo;

import com.juege.oshrelease.model.Environment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvironmentRepository extends JpaRepository<Environment, Long> {
    Optional<Environment> findByEnvCode(String envCode);
}

