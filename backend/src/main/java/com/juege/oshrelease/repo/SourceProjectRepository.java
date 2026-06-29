package com.juege.oshrelease.repo;

import com.juege.oshrelease.model.SourceProject;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceProjectRepository extends JpaRepository<SourceProject, Long> {
    Optional<SourceProject> findByProjectKey(String projectKey);
}

