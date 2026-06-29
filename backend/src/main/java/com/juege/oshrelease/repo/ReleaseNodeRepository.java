package com.juege.oshrelease.repo;

import com.juege.oshrelease.model.ReleaseNode;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseNodeRepository extends JpaRepository<ReleaseNode, Long> {
    List<ReleaseNode> findByChangeIdOrderByNodeOrderAsc(Long changeId);
}

