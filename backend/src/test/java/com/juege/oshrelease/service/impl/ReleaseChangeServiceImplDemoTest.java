package com.juege.oshrelease.service.impl;

import com.juege.oshrelease.common.ChangeStatus;
import com.juege.oshrelease.common.ReleaseType;
import com.juege.oshrelease.dto.ReleaseChangeDetailDTO;
import com.juege.oshrelease.dto.ReleaseChangeOperationRequest;
import com.juege.oshrelease.model.AppUser;
import com.juege.oshrelease.model.ComponentDefinition;
import com.juege.oshrelease.model.DemoRecord;
import com.juege.oshrelease.model.Environment;
import com.juege.oshrelease.model.ReleaseChange;
import com.juege.oshrelease.repo.ComponentRepository;
import com.juege.oshrelease.repo.DemoRecordRepository;
import com.juege.oshrelease.repo.EnvironmentRepository;
import com.juege.oshrelease.repo.ReleaseChangeItemRepository;
import com.juege.oshrelease.repo.ReleaseChangeRepository;
import com.juege.oshrelease.repo.ReleaseNodeRepository;
import com.juege.oshrelease.repo.ReleaseOperationRecordRepository;
import com.juege.oshrelease.repo.ReviewRecordRepository;
import com.juege.oshrelease.repo.ReviewerTestEvidenceRepository;
import com.juege.oshrelease.repo.TestReportRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ReleaseChangeServiceImplDemoTest {

    @Test
    void demoShouldPersistRecordAndExposeItInDetail() {
        ReleaseChangeRepository releaseChangeRepository = Mockito.mock(ReleaseChangeRepository.class);
        ReleaseNodeRepository releaseNodeRepository = Mockito.mock(ReleaseNodeRepository.class);
        ReleaseChangeItemRepository releaseChangeItemRepository = Mockito.mock(ReleaseChangeItemRepository.class);
        ReviewRecordRepository reviewRecordRepository = Mockito.mock(ReviewRecordRepository.class);
        ReviewerTestEvidenceRepository reviewerTestEvidenceRepository = Mockito.mock(ReviewerTestEvidenceRepository.class);
        DemoRecordRepository demoRecordRepository = Mockito.mock(DemoRecordRepository.class);
        TestReportRepository testReportRepository = Mockito.mock(TestReportRepository.class);
        ReleaseOperationRecordRepository releaseOperationRecordRepository = Mockito.mock(ReleaseOperationRecordRepository.class);
        ComponentRepository componentRepository = Mockito.mock(ComponentRepository.class);
        EnvironmentRepository environmentRepository = Mockito.mock(EnvironmentRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        List<DemoRecord> storedDemoRecords = new CopyOnWriteArrayList<DemoRecord>();

        ReleaseChangeServiceImpl service = new ReleaseChangeServiceImpl(
                releaseChangeRepository,
                releaseNodeRepository,
                releaseChangeItemRepository,
                reviewRecordRepository,
                reviewerTestEvidenceRepository,
                demoRecordRepository,
                testReportRepository,
                releaseOperationRecordRepository,
                componentRepository,
                environmentRepository
        );

        ReleaseChange change = new ReleaseChange();
        change.setId(1L);
        change.setChangeCode("CHG-1");
        change.setTitle("demo");
        change.setProjectBranch("release/20260708");
        change.setReleaseType(ReleaseType.NORMAL);
        change.setTargetEnvCode("prod");
        change.setTargetColor("green");
        change.setStatus(ChangeStatus.REVIEWING);
        change.setDeveloperUsername("reviewer_a");
        change.setDeveloperDisplayName("评审 A");
        change.setReviewMode("TWO_REVIEWERS_PLUS_JUEGE");
        change.setDemoRequired(true);
        change.setDemoConfirmed(false);
        change.setRiskLevel("HIGH");
        change.setSummary("demo");
        change.setContentJson("{}");
        change.setCurrentStep("review");
        change.setFinalMessage("review");

        Environment env = new Environment();
        env.setEnvCode("prod");
        env.setEnvName("生产环境");
        env.setEnvKind("PROD");
        env.setBaseUrl("https://osh.lol/");
        env.setCurrentColor("blue");
        env.setSupportsBlueGreen(true);
        env.setAnnounceFileExists(false);
        env.setHealthStatus("PROTECTED");
        env.setNotes("prod");

        when(releaseChangeRepository.findById(1L)).thenReturn(Optional.of(change));
        when(environmentRepository.findByEnvCode("prod")).thenReturn(Optional.of(env));
        when(demoRecordRepository.findByChangeIdOrderByCreatedAtAsc(1L)).thenAnswer(invocation -> new java.util.ArrayList<DemoRecord>(storedDemoRecords));
        when(releaseNodeRepository.findByChangeIdOrderByNodeOrderAsc(1L)).thenReturn(Collections.emptyList());
        when(releaseChangeItemRepository.findByChangeIdOrderByItemOrderAsc(1L)).thenReturn(Collections.emptyList());
        when(reviewRecordRepository.findByChangeIdOrderByCreatedAtAsc(1L)).thenReturn(Collections.emptyList());
        when(reviewerTestEvidenceRepository.findByChangeIdOrderByCreatedAtAsc(1L)).thenReturn(Collections.emptyList());
        when(testReportRepository.findByChangeIdOrderByCreatedAtAsc(1L)).thenReturn(Collections.emptyList());
        when(releaseOperationRecordRepository.findByChangeIdOrderByCreatedAtAsc(1L)).thenReturn(Collections.emptyList());
        when(componentRepository.findByComponentKey(anyString())).thenReturn(Optional.of(component()));
        when(releaseChangeRepository.save(any(ReleaseChange.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(demoRecordRepository.save(any(DemoRecord.class))).thenAnswer(invocation -> {
            DemoRecord record = invocation.getArgument(0);
            storedDemoRecords.add(record);
            return record;
        });

        ReleaseChangeOperationRequest request = new ReleaseChangeOperationRequest();
        request.reviewerUsername = "reviewer_b";
        request.reviewerDisplayName = "评审 B";
        request.actorUsername = "reviewer_a";
        request.actorDisplayName = "评审 A";
        request.comment = "评审 A 已向评审 B 演示";

        ReleaseChangeDetailDTO detail = service.demo(1L, request);

        assertTrue(detail.demoConfirmed);
        assertNotNull(detail.demos);
        assertEquals(1, detail.demos.size());
        assertEquals("reviewer_a", detail.demos.get(0).developerUsername);
        assertEquals("reviewer_b", detail.demos.get(0).reviewerUsername);
        assertFalse(detail.demos.get(0).content.isEmpty());
    }

    private ComponentDefinition component() {
        ComponentDefinition component = new ComponentDefinition();
        component.setComponentKey("mysql");
        component.setComponentName("MySQL");
        component.setComponentType("DATABASE");
        component.setConfigDir("/data/osh/config/mysql");
        component.setDataDir("/data/osh/data/mysql");
        component.setDeployPath("/data/osh/compose/mysql");
        component.setSupportIncremental(true);
        component.setSupportRollback(true);
        component.setSupportBlueGreen(true);
        component.setExtension(false);
        component.setCore(true);
        component.setInstallOrder(10);
        component.setRollbackOrder(90);
        component.setNotes("mysql");
        return component;
    }
}
