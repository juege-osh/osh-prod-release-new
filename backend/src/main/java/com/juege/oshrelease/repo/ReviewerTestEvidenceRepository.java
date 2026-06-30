package com.juege.oshrelease.repo;

import com.juege.oshrelease.model.ReviewerTestEvidence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewerTestEvidenceRepository extends JpaRepository<ReviewerTestEvidence, Long> {
    List<ReviewerTestEvidence> findByChangeIdOrderByCreatedAtAsc(Long changeId);
}
