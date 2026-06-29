package com.juege.oshrelease.repo;

import com.juege.oshrelease.model.ComponentDefinition;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComponentRepository extends JpaRepository<ComponentDefinition, Long> {
    Optional<ComponentDefinition> findByComponentKey(String componentKey);
}

