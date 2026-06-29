package com.juege.oshrelease.repo;

import com.juege.oshrelease.model.ReviewRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRecordRepository extends JpaRepository<ReviewRecord, Long> {
    List<ReviewRecord> findByChangeIdOrderByCreatedAtAsc(Long changeId);
}

