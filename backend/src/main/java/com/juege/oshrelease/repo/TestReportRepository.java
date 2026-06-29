package com.juege.oshrelease.repo;

import com.juege.oshrelease.model.TestReport;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestReportRepository extends JpaRepository<TestReport, Long> {
    List<TestReport> findByChangeIdOrderByCreatedAtAsc(Long changeId);
}

