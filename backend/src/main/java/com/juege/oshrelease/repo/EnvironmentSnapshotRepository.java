package com.juege.oshrelease.repo;

import com.juege.oshrelease.model.EnvironmentSnapshot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvironmentSnapshotRepository extends JpaRepository<EnvironmentSnapshot, Long> {
    List<EnvironmentSnapshot> findByEnvCodeOrderByCreatedAtAsc(String envCode);
}

