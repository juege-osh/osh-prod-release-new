package com.juege.oshrelease.repo;

import com.juege.oshrelease.model.ReleaseChange;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseChangeRepository extends JpaRepository<ReleaseChange, Long> {
    Optional<ReleaseChange> findByChangeCode(String changeCode);
}

