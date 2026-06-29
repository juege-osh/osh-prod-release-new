package com.juege.oshrelease.repo;

import com.juege.oshrelease.model.DemoRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemoRecordRepository extends JpaRepository<DemoRecord, Long> {
    List<DemoRecord> findByChangeIdOrderByCreatedAtAsc(Long changeId);
}

