package com.juege.oshrelease.repo;

import com.juege.oshrelease.model.ReleaseOperationRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseOperationRecordRepository extends JpaRepository<ReleaseOperationRecord, Long> {
    List<ReleaseOperationRecord> findByChangeIdOrderByCreatedAtAsc(Long changeId);
}
