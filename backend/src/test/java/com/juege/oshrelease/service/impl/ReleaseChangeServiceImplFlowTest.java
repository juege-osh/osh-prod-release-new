package com.juege.oshrelease.service.impl;

import com.juege.oshrelease.common.ChangeStatus;
import com.juege.oshrelease.common.ReleaseType;
import com.juege.oshrelease.dto.ReleaseChangeCreateRequest;
import com.juege.oshrelease.dto.ReleaseChangeDetailDTO;
import com.juege.oshrelease.dto.ReleaseChangeItemCreateRequest;
import com.juege.oshrelease.dto.ReleaseChangeItemUpdateRequest;
import com.juege.oshrelease.dto.ReleaseChangeOperationRequest;
import com.juege.oshrelease.dto.ReviewerTestEvidenceRequest;
import com.juege.oshrelease.model.ComponentDefinition;
import com.juege.oshrelease.model.DemoRecord;
import com.juege.oshrelease.model.Environment;
import com.juege.oshrelease.model.ReleaseChange;
import com.juege.oshrelease.model.ReleaseChangeItem;
import com.juege.oshrelease.model.ReleaseNode;
import com.juege.oshrelease.model.ReleaseOperationRecord;
import com.juege.oshrelease.model.ReviewerTestEvidence;
import com.juege.oshrelease.model.ReviewRecord;
import com.juege.oshrelease.model.TestReport;
import com.juege.oshrelease.repo.ComponentRepository;
import com.juege.oshrelease.repo.DemoRecordRepository;
import com.juege.oshrelease.repo.EnvironmentRepository;
import com.juege.oshrelease.repo.ReleaseChangeItemRepository;
import com.juege.oshrelease.repo.ReleaseChangeRepository;
import com.juege.oshrelease.repo.ReleaseNodeRepository;
import com.juege.oshrelease.repo.ReleaseOperationRecordRepository;
import com.juege.oshrelease.repo.ReviewerTestEvidenceRepository;
import com.juege.oshrelease.repo.ReviewRecordRepository;
import com.juege.oshrelease.repo.TestReportRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ReleaseChangeServiceImplFlowTest {

    @Test
    void shouldRunFullNormalFlowAndExposeReports() {
        Fixture fixture = new Fixture(ReleaseType.NORMAL);
        ReleaseChangeServiceImpl service = fixture.service;

        ReleaseChangeCreateRequest createRequest = new ReleaseChangeCreateRequest();
        createRequest.title = "release flow";
        createRequest.projectBranch = "release/20260708";
        createRequest.releaseType = ReleaseType.NORMAL;
        createRequest.targetEnvCode = "prod";
        createRequest.targetColor = "green";
        createRequest.developerUsername = "reviewer_a";
        createRequest.developerDisplayName = "评审 A";
        createRequest.demoRequired = true;
        createRequest.riskLevel = "HIGH";
        createRequest.summary = "full flow";
        createRequest.componentKeys = Arrays.asList("mysql", "redis");
        createRequest.contentJson = "{}";

        ReleaseChangeDetailDTO created = service.create(createRequest);
        assertNotNull(created.changeCode);
        assertEquals(2, created.nodes.size());
        assertEquals(2, created.items.size());
        assertEquals("node-mysql-1", created.nodes.get(0).nodeKey);

        ReleaseChangeItemCreateRequest sqlItem = new ReleaseChangeItemCreateRequest();
        sqlItem.itemType = "SQL";
        sqlItem.componentKey = "mysql";
        sqlItem.title = "给订单表补状态字段";
        sqlItem.payloadPath = "mysql/release/20260708/add-order-status.sql";
        sqlItem.changeContent = "新增订单状态字段，不改课程和用户模块。";
        sqlItem.executionContent = "alter table order_info add column status varchar(32) default 'CREATED';";
        sqlItem.rollbackContent = "alter table order_info drop column status;";
        sqlItem.incrementalPlan = "先备份表结构，再在绿环境 dry-run。";
        sqlItem.rollbackPlan = "回滚时先确认应用已退回旧版本，再 drop 字段。";
        sqlItem.codeChangeSummary = "后端读取 status 字段，老数据默认 CREATED。";
        sqlItem.riskAnalysis = "风险在订单查询兼容性；课程和用户模块不受影响。";
        sqlItem.bugAnalysis = "注意默认值、老版本代码读写兼容和索引影响。";
        sqlItem.verificationCommands = "select count(*) from order_info where status is null;";
        sqlItem.testPlan = "测试订单列表、订单详情和回滚 SQL。";
        sqlItem.dataProbePlan = "对比 order_info 行数；课程和用户表变化必须为 0。";
        ReleaseChangeDetailDTO afterSqlItem = service.createItem(created.id, sqlItem);
        assertEquals(3, afterSqlItem.items.size());
        assertEquals("SQL", afterSqlItem.items.get(2).itemType);
        assertTrue(afterSqlItem.items.get(2).executionContent.contains("alter table"));
        assertTrue(afterSqlItem.items.get(2).bugAnalysis.contains("兼容"));
        assertEquals(3, afterSqlItem.nodes.size());
        assertEquals("node-mysql-3", afterSqlItem.nodes.get(2).nodeKey);

        ReleaseChangeItemUpdateRequest sqlUpdate = sqlItemUpdateRequest();
        ReleaseChangeDetailDTO afterSqlUpdate = service.updateItem(created.id, afterSqlItem.items.get(2).id, sqlUpdate);
        assertEquals("MYSQL", afterSqlUpdate.nodes.get(0).componentName);
        assertEquals("订单状态字段 SQL 上线", afterSqlUpdate.nodes.get(2).componentName);
        assertTrue(afterSqlUpdate.nodes.get(2).detailJson.contains("idx_order_info_status"));

        service.submit(created.id);

        ReleaseChangeDetailDTO afterUpdate = service.updateItem(created.id, created.items.get(0).id, itemRequest());
        assertEquals("ops", afterUpdate.items.get(0).ownerUsername);
        assertEquals("READY", afterUpdate.items.get(0).specStatus);

        ReleaseChangeOperationRequest demoRequest = new ReleaseChangeOperationRequest();
        demoRequest.reviewerUsername = "reviewer_b";
        demoRequest.reviewerDisplayName = "评审 B";
        demoRequest.actorUsername = "reviewer_a";
        demoRequest.actorDisplayName = "评审 A";
        demoRequest.comment = "评审 A 已向评审 B 演示";
        ReleaseChangeDetailDTO afterDemo = service.demo(created.id, demoRequest);
        assertTrue(afterDemo.demoConfirmed);
        assertEquals(1, afterDemo.demos.size());

        ReleaseChangeOperationRequest reviewA = new ReleaseChangeOperationRequest();
        reviewA.reviewerUsername = "reviewer_a";
        reviewA.reviewerDisplayName = "评审 A";
        reviewA.passed = true;
        reviewA.comment = "A passed";
        service.approve(created.id, reviewA);

        ReleaseChangeOperationRequest reviewB = new ReleaseChangeOperationRequest();
        reviewB.reviewerUsername = "reviewer_b";
        reviewB.reviewerDisplayName = "评审 B";
        reviewB.passed = true;
        reviewB.comment = "B passed";
        service.approve(created.id, reviewB);

        ReleaseChangeOperationRequest juege = new ReleaseChangeOperationRequest();
        juege.reviewerUsername = "juege";
        juege.reviewerDisplayName = "觉哥";
        juege.passed = true;
        juege.comment = "final";
        ReleaseChangeDetailDTO approved = service.approve(created.id, juege);
        assertEquals(ChangeStatus.APPROVED.name(), approved.status);

        ensureReviewerEvidence(service, created.id, created.items.get(0).id, "reviewer_a", "评审 A", "test", "mysql A");
        ensureReviewerEvidence(service, created.id, created.items.get(0).id, "reviewer_b", "评审 B", "prod-green", "mysql B");
        ensureReviewerEvidence(service, created.id, created.items.get(1).id, "reviewer_a", "评审 A", "test", "redis A");
        ensureReviewerEvidence(service, created.id, created.items.get(1).id, "reviewer_b", "评审 B", "prod-green", "redis B");
        ensureReviewerEvidence(service, created.id, afterSqlItem.items.get(2).id, "reviewer_a", "评审 A", "test", "sql A");
        ensureReviewerEvidence(service, created.id, afterSqlItem.items.get(2).id, "reviewer_b", "评审 B", "prod-green", "sql B");

        service.validateSpecs(created.id);
        ReleaseChangeDetailDTO function = service.functionTest(created.id);
        assertTrue(function.reports.stream().anyMatch(item -> "FUNCTION".equals(item.reportType) && item.passed));

        ReleaseChangeDetailDTO data = service.dataTest(created.id);
        assertTrue(data.reports.stream().anyMatch(item -> "DATA".equals(item.reportType) && item.passed));

        service.envDiff(created.id);
        service.announceCheck(created.id);
        MapResult readyReports = new MapResult(service.reports(created.id));
        assertTrue(readyReports.booleanValue("specPassed"));
        assertTrue(readyReports.booleanValue("functionalPassed"));
        assertTrue(readyReports.booleanValue("dataPassed"));
        assertTrue(readyReports.booleanValue("envDiffGenerated"));
        assertTrue(readyReports.booleanValue("announceChecked"));
        assertTrue(readyReports.booleanValue("readyForGreen"));

        ReleaseChangeDetailDTO switched = service.switchGreen(created.id);
        assertEquals(ChangeStatus.SWITCHED.name(), switched.status);
        assertEquals("green", fixture.env.getCurrentColor());

        ReleaseChangeOperationRequest manual = new ReleaseChangeOperationRequest();
        manual.actorUsername = "ops";
        manual.actorDisplayName = "运维同学";
        manual.comment = "人工验证通过";
        ReleaseChangeDetailDTO verified = service.manualVerify(created.id, manual);
        assertEquals(ChangeStatus.VERIFIED.name(), verified.status);

        ReleaseChangeDetailDTO synced = service.syncGreenToBlue(created.id, manual);
        assertEquals(ChangeStatus.RELEASED.name(), synced.status);

        ReleaseChangeDetailDTO rolledBack = service.rollback(created.id);
        assertEquals(ChangeStatus.ROLLED_BACK.name(), rolledBack.status);
        assertTrue(rolledBack.operations.stream().anyMatch(item -> "NODE_ROLLBACK".equals(item.operationType)));

        MapResult reports = new MapResult(service.reports(created.id));
        assertFalse(reports.booleanValue("readyForGreen"));
    }

    @Test
    void urgentFlowShouldRequireJuegeEvidenceBeforeTesting() {
        Fixture fixture = new Fixture(ReleaseType.URGENT);
        ReleaseChangeServiceImpl service = fixture.service;

        ReleaseChangeCreateRequest request = new ReleaseChangeCreateRequest();
        request.title = "urgent flow";
        request.releaseType = ReleaseType.URGENT;
        request.targetEnvCode = "prod";
        request.targetColor = "green";
        request.developerUsername = "reviewer_a";
        request.developerDisplayName = "评审 A";
        request.demoRequired = true;
        request.riskLevel = "HIGH";
        request.summary = "urgent";
        request.componentKeys = Collections.singletonList("mysql");
        request.contentJson = "{}";
        ReleaseChangeDetailDTO created = service.create(request);
        service.submit(created.id);

        ReleaseChangeOperationRequest demo = new ReleaseChangeOperationRequest();
        demo.reviewerUsername = "reviewer_b";
        demo.reviewerDisplayName = "评审 B";
        demo.actorUsername = "reviewer_a";
        demo.actorDisplayName = "评审 A";
        demo.comment = "demo";
        service.demo(created.id, demo);

        ReleaseChangeOperationRequest reviewA = new ReleaseChangeOperationRequest();
        reviewA.reviewerUsername = "reviewer_a";
        reviewA.reviewerDisplayName = "评审 A";
        reviewA.passed = true;
        service.approve(created.id, reviewA);
        ReleaseChangeOperationRequest reviewB = new ReleaseChangeOperationRequest();
        reviewB.reviewerUsername = "reviewer_b";
        reviewB.reviewerDisplayName = "评审 B";
        reviewB.passed = true;
        service.approve(created.id, reviewB);
        ReleaseChangeOperationRequest juege = new ReleaseChangeOperationRequest();
        juege.reviewerUsername = "juege";
        juege.reviewerDisplayName = "觉哥";
        juege.passed = true;
        service.approve(created.id, juege);

        ensureReviewerEvidence(service, created.id, created.items.get(0).id, "reviewer_a", "评审 A", "test", "mysql A");
        ensureReviewerEvidence(service, created.id, created.items.get(0).id, "reviewer_b", "评审 B", "prod-green", "mysql B");

        assertThrows(com.juege.oshrelease.common.BusinessException.class, () -> service.functionTest(created.id));

        ensureReviewerEvidence(service, created.id, created.items.get(0).id, "juege", "觉哥", "prod-green", "mysql juege");
        service.validateSpecs(created.id);
        ReleaseChangeDetailDTO function = service.functionTest(created.id);
        assertTrue(function.reports.stream().anyMatch(item -> "FUNCTION".equals(item.reportType)));
    }

    @Test
    void switchGreenShouldRequireEnvDiffAndAnnounceReports() {
        Fixture fixture = new Fixture(ReleaseType.NORMAL);
        ReleaseChangeServiceImpl service = fixture.service;

        ReleaseChangeCreateRequest createRequest = new ReleaseChangeCreateRequest();
        createRequest.title = "release gate";
        createRequest.releaseType = ReleaseType.NORMAL;
        createRequest.targetEnvCode = "prod";
        createRequest.targetColor = "green";
        createRequest.developerUsername = "reviewer_a";
        createRequest.developerDisplayName = "评审 A";
        createRequest.demoRequired = true;
        createRequest.riskLevel = "HIGH";
        createRequest.summary = "gate";
        createRequest.componentKeys = Collections.singletonList("mysql");
        createRequest.contentJson = "{}";

        ReleaseChangeDetailDTO created = service.create(createRequest);
        service.submit(created.id);

        ReleaseChangeOperationRequest demo = new ReleaseChangeOperationRequest();
        demo.reviewerUsername = "reviewer_b";
        demo.reviewerDisplayName = "评审 B";
        demo.actorUsername = "reviewer_a";
        demo.actorDisplayName = "评审 A";
        demo.comment = "demo";
        service.demo(created.id, demo);

        ReleaseChangeOperationRequest reviewA = new ReleaseChangeOperationRequest();
        reviewA.reviewerUsername = "reviewer_a";
        reviewA.reviewerDisplayName = "评审 A";
        reviewA.passed = true;
        service.approve(created.id, reviewA);
        ReleaseChangeOperationRequest reviewB = new ReleaseChangeOperationRequest();
        reviewB.reviewerUsername = "reviewer_b";
        reviewB.reviewerDisplayName = "评审 B";
        reviewB.passed = true;
        service.approve(created.id, reviewB);
        ReleaseChangeOperationRequest juege = new ReleaseChangeOperationRequest();
        juege.reviewerUsername = "juege";
        juege.reviewerDisplayName = "觉哥";
        juege.passed = true;
        service.approve(created.id, juege);

        ensureReviewerEvidence(service, created.id, created.items.get(0).id, "reviewer_a", "评审 A", "test", "mysql A");
        ensureReviewerEvidence(service, created.id, created.items.get(0).id, "reviewer_b", "评审 B", "prod-green", "mysql B");

        service.validateSpecs(created.id);
        service.functionTest(created.id);
        service.dataTest(created.id);

        MapResult blockedReports = new MapResult(service.reports(created.id));
        assertFalse(blockedReports.booleanValue("readyForGreen"));
        com.juege.oshrelease.common.BusinessException blocked = assertThrows(
                com.juege.oshrelease.common.BusinessException.class,
                () -> service.switchGreen(created.id)
        );
        assertTrue(blocked.getMessage().contains("环境差异报告"));

        service.envDiff(created.id);
        service.announceCheck(created.id);

        MapResult readyReports = new MapResult(service.reports(created.id));
        assertTrue(readyReports.booleanValue("readyForGreen"));
        ReleaseChangeDetailDTO switched = service.switchGreen(created.id);
        assertEquals(ChangeStatus.SWITCHED.name(), switched.status);
    }

    @Test
    void switchBlueShouldRefuseBeforeSwitchGreen() {
        Fixture fixture = new Fixture(ReleaseType.NORMAL);
        ReleaseChangeServiceImpl service = fixture.service;

        ReleaseChange change = fixture.change;
        assertThrows(com.juege.oshrelease.common.BusinessException.class, () -> service.switchBlue(change.getId()));
    }

    private ReleaseChangeItemUpdateRequest itemRequest() {
        ReleaseChangeItemUpdateRequest request = new ReleaseChangeItemUpdateRequest();
        request.ownerUsername = "ops";
        request.ownerDisplayName = "运维同学";
        request.title = "MySQL 增量上线";
        request.changeContent = "更新后的上线内容";
        request.incrementalPlan = "先 dry-run";
        request.rollbackPlan = "按顺序回滚";
        request.testPlan = "测试通过";
        request.dataProbePlan = "数据摘要通过";
        return request;
    }

    private ReleaseChangeItemUpdateRequest sqlItemUpdateRequest() {
        ReleaseChangeItemUpdateRequest request = new ReleaseChangeItemUpdateRequest();
        request.ownerUsername = "ops";
        request.ownerDisplayName = "运维同学";
        request.title = "订单状态字段 SQL 上线";
        request.itemType = "SQL";
        request.payloadPath = "mysql/release/20260708/add-order-status-v2.sql";
        request.changeContent = "新增订单状态索引，不改课程和用户模块。";
        request.executionContent = "create index idx_order_info_status on order_info(status);";
        request.incrementalPlan = "先 explain，再绿环境 dry-run。";
        request.rollbackContent = "drop index idx_order_info_status;";
        request.rollbackPlan = "先确认查询已回退，再删除索引。";
        request.codeChangeSummary = "非代码上线项；后端仅使用已有 status 字段。";
        request.riskAnalysis = "风险在索引创建期间锁表；课程和用户模块不受影响。";
        request.bugAnalysis = "注意重复索引、锁等待、低版本数据库语法差异。";
        request.verificationCommands = "show index from order_info where Key_name = 'idx_order_info_status';";
        request.testPlan = "验证订单列表查询和索引回滚。";
        request.dataProbePlan = "对比 order_info 行数；课程和用户表变化必须为 0。";
        return request;
    }

    private void ensureReviewerEvidence(ReleaseChangeServiceImpl service, Long changeId, Long itemId,
                                        String username, String displayName, String envCode, String evidenceText) {
        ReviewerTestEvidenceRequest request = new ReviewerTestEvidenceRequest();
        request.itemId = itemId;
        request.reviewerUsername = username;
        request.reviewerDisplayName = displayName;
        request.testType = "FUNCTION";
        request.environmentCode = envCode;
        request.passed = true;
        request.demoObserved = true;
        request.responsibilityAccepted = true;
        request.evidence = evidenceText;
        ReleaseChangeDetailDTO detail = service.reviewerTest(changeId, request);
        assertFalse(detail.evidences.isEmpty());
    }

    private static class Fixture {
        final ReleaseChangeServiceImpl service;
        final Environment env;
        final ReleaseChange change;
        final Map<Long, ReleaseChange> changeStore = new HashMap<Long, ReleaseChange>();
        final Map<Long, List<ReleaseNode>> nodeStore = new HashMap<Long, List<ReleaseNode>>();
        final Map<Long, List<ReleaseChangeItem>> itemStore = new HashMap<Long, List<ReleaseChangeItem>>();
        final Map<Long, List<ReviewRecord>> reviewStore = new HashMap<Long, List<ReviewRecord>>();
        final Map<Long, List<ReviewerTestEvidence>> evidenceStore = new HashMap<Long, List<ReviewerTestEvidence>>();
        final Map<Long, List<DemoRecord>> demoStore = new HashMap<Long, List<DemoRecord>>();
        final Map<Long, List<TestReport>> reportStore = new HashMap<Long, List<TestReport>>();
        final Map<Long, List<ReleaseOperationRecord>> operationStore = new HashMap<Long, List<ReleaseOperationRecord>>();
        final Map<Long, ReleaseChangeItem> itemById = new HashMap<Long, ReleaseChangeItem>();
        final Map<Long, ReleaseNode> nodeById = new HashMap<Long, ReleaseNode>();
        final Map<String, Environment> environmentStore = new HashMap<String, Environment>();

        Fixture(ReleaseType releaseType) {
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
            List<DemoRecord> demoRecords = new CopyOnWriteArrayList<DemoRecord>();
            List<ReviewRecord> reviewRecords = new CopyOnWriteArrayList<ReviewRecord>();
            List<ReviewerTestEvidence> evidences = new CopyOnWriteArrayList<ReviewerTestEvidence>();
            List<TestReport> reports = new CopyOnWriteArrayList<TestReport>();
            List<ReleaseOperationRecord> operations = new CopyOnWriteArrayList<ReleaseOperationRecord>();
            AtomicLong idSeq = new AtomicLong(100L);

            this.service = new ReleaseChangeServiceImpl(
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

            this.env = environment();
            environmentStore.put(env.getEnvCode(), env);
            environmentStore.put("test", testEnvironment());
            this.change = buildChange(releaseType);
            changeStore.put(change.getId(), change);

            when(environmentRepository.findByEnvCode(anyString())).thenAnswer(invocation -> Optional.ofNullable(environmentStore.get(invocation.getArgument(0))));
            when(environmentRepository.save(any(Environment.class))).thenAnswer(invocation -> {
                Environment saved = invocation.getArgument(0);
                environmentStore.put(saved.getEnvCode(), saved);
                return saved;
            });
            when(componentRepository.findByComponentKey(anyString())).thenAnswer(invocation -> Optional.of(component(invocation.getArgument(0))));
            when(releaseChangeRepository.findById(any())).thenAnswer(invocation -> Optional.ofNullable(changeStore.get(invocation.getArgument(0))));
            when(releaseChangeRepository.findByChangeCode(anyString())).thenAnswer(invocation -> {
                String changeCode = invocation.getArgument(0);
                for (ReleaseChange stored : changeStore.values()) {
                    if (stored.getChangeCode().equals(changeCode)) {
                        return Optional.of(stored);
                    }
                }
                return Optional.empty();
            });
            when(releaseChangeRepository.save(any(ReleaseChange.class))).thenAnswer(invocation -> {
                ReleaseChange saved = invocation.getArgument(0);
                if (saved.getId() == null) {
                    saved.setId(idSeq.getAndIncrement());
                }
                changeStore.put(saved.getId(), saved);
                return saved;
            });
            when(releaseNodeRepository.findByChangeIdOrderByNodeOrderAsc(any())).thenAnswer(invocation -> {
                Long changeId = invocation.getArgument(0);
                List<ReleaseNode> stored = nodeStore.get(changeId);
                return stored == null ? Collections.<ReleaseNode>emptyList() : new ArrayList<ReleaseNode>(stored);
            });
            when(releaseNodeRepository.save(any(ReleaseNode.class))).thenAnswer(invocation -> {
                ReleaseNode saved = invocation.getArgument(0);
                if (saved.getId() == null) {
                    saved.setId(idSeq.getAndIncrement());
                }
                nodeById.put(saved.getId(), saved);
                List<ReleaseNode> stored = nodeStore.get(saved.getChangeId());
                if (stored == null) {
                    stored = new CopyOnWriteArrayList<ReleaseNode>();
                    nodeStore.put(saved.getChangeId(), stored);
                }
                if (!stored.contains(saved)) {
                    stored.add(saved);
                }
                return saved;
            });
            when(releaseChangeItemRepository.findByChangeIdOrderByItemOrderAsc(any())).thenAnswer(invocation -> {
                Long changeId = invocation.getArgument(0);
                List<ReleaseChangeItem> stored = itemStore.get(changeId);
                return stored == null ? Collections.<ReleaseChangeItem>emptyList() : new ArrayList<ReleaseChangeItem>(stored);
            });
            when(releaseChangeItemRepository.findById(any())).thenAnswer(invocation -> {
                Long itemId = invocation.getArgument(0);
                return Optional.ofNullable(itemById.get(itemId));
            });
            when(releaseChangeItemRepository.save(any(ReleaseChangeItem.class))).thenAnswer(invocation -> {
                ReleaseChangeItem saved = invocation.getArgument(0);
                if (saved.getId() == null) {
                    saved.setId(idSeq.getAndIncrement());
                }
                itemById.put(saved.getId(), saved);
                List<ReleaseChangeItem> stored = itemStore.get(saved.getChangeId());
                if (stored == null) {
                    stored = new CopyOnWriteArrayList<ReleaseChangeItem>();
                    itemStore.put(saved.getChangeId(), stored);
                }
                if (!stored.contains(saved)) {
                    stored.add(saved);
                }
                return saved;
            });
            when(reviewRecordRepository.findByChangeIdOrderByCreatedAtAsc(any())).thenAnswer(invocation -> {
                Long changeId = invocation.getArgument(0);
                List<ReviewRecord> stored = reviewStore.get(changeId);
                return stored == null ? Collections.<ReviewRecord>emptyList() : new ArrayList<ReviewRecord>(stored);
            });
            when(reviewRecordRepository.save(any(ReviewRecord.class))).thenAnswer(invocation -> {
                ReviewRecord record = invocation.getArgument(0);
                if (record.getId() == null) {
                    record.setId(idSeq.getAndIncrement());
                }
                List<ReviewRecord> stored = reviewStore.get(record.getChangeId());
                if (stored == null) {
                    stored = new CopyOnWriteArrayList<ReviewRecord>();
                    reviewStore.put(record.getChangeId(), stored);
                }
                if (!stored.contains(record)) {
                    stored.add(record);
                }
                return record;
            });
            when(reviewerTestEvidenceRepository.findByChangeIdOrderByCreatedAtAsc(any())).thenAnswer(invocation -> {
                Long changeId = invocation.getArgument(0);
                List<ReviewerTestEvidence> stored = evidenceStore.get(changeId);
                return stored == null ? Collections.<ReviewerTestEvidence>emptyList() : new ArrayList<ReviewerTestEvidence>(stored);
            });
            when(reviewerTestEvidenceRepository.save(any(ReviewerTestEvidence.class))).thenAnswer(invocation -> {
                ReviewerTestEvidence evidence = invocation.getArgument(0);
                if (evidence.getId() == null) {
                    evidence.setId(idSeq.getAndIncrement());
                }
                List<ReviewerTestEvidence> stored = evidenceStore.get(evidence.getChangeId());
                if (stored == null) {
                    stored = new CopyOnWriteArrayList<ReviewerTestEvidence>();
                    evidenceStore.put(evidence.getChangeId(), stored);
                }
                if (!stored.contains(evidence)) {
                    stored.add(evidence);
                }
                return evidence;
            });
            when(demoRecordRepository.findByChangeIdOrderByCreatedAtAsc(any())).thenAnswer(invocation -> {
                Long changeId = invocation.getArgument(0);
                List<DemoRecord> stored = demoStore.get(changeId);
                return stored == null ? Collections.<DemoRecord>emptyList() : new ArrayList<DemoRecord>(stored);
            });
            when(demoRecordRepository.save(any(DemoRecord.class))).thenAnswer(invocation -> {
                DemoRecord record = invocation.getArgument(0);
                if (record.getId() == null) {
                    record.setId(idSeq.getAndIncrement());
                }
                List<DemoRecord> stored = demoStore.get(record.getChangeId());
                if (stored == null) {
                    stored = new CopyOnWriteArrayList<DemoRecord>();
                    demoStore.put(record.getChangeId(), stored);
                }
                if (!stored.contains(record)) {
                    stored.add(record);
                }
                return record;
            });
            when(testReportRepository.findByChangeIdOrderByCreatedAtAsc(any())).thenAnswer(invocation -> {
                Long changeId = invocation.getArgument(0);
                List<TestReport> stored = reportStore.get(changeId);
                return stored == null ? Collections.<TestReport>emptyList() : new ArrayList<TestReport>(stored);
            });
            when(testReportRepository.save(any(TestReport.class))).thenAnswer(invocation -> {
                TestReport report = invocation.getArgument(0);
                if (report.getId() == null) {
                    report.setId(idSeq.getAndIncrement());
                }
                List<TestReport> stored = reportStore.get(report.getChangeId());
                if (stored == null) {
                    stored = new CopyOnWriteArrayList<TestReport>();
                    reportStore.put(report.getChangeId(), stored);
                }
                if (!stored.contains(report)) {
                    stored.add(report);
                }
                return report;
            });
            when(releaseOperationRecordRepository.findByChangeIdOrderByCreatedAtAsc(any())).thenAnswer(invocation -> {
                Long changeId = invocation.getArgument(0);
                List<ReleaseOperationRecord> stored = operationStore.get(changeId);
                return stored == null ? Collections.<ReleaseOperationRecord>emptyList() : new ArrayList<ReleaseOperationRecord>(stored);
            });
            when(releaseOperationRecordRepository.save(any(ReleaseOperationRecord.class))).thenAnswer(invocation -> {
                ReleaseOperationRecord operation = invocation.getArgument(0);
                if (operation.getId() == null) {
                    operation.setId(idSeq.getAndIncrement());
                }
                List<ReleaseOperationRecord> stored = operationStore.get(operation.getChangeId());
                if (stored == null) {
                    stored = new CopyOnWriteArrayList<ReleaseOperationRecord>();
                    operationStore.put(operation.getChangeId(), stored);
                }
                if (!stored.contains(operation)) {
                    stored.add(operation);
                }
                return operation;
            });
        }

        private ReleaseChange buildChange(ReleaseType releaseType) {
            ReleaseChange change = new ReleaseChange();
            change.setId(1L);
            change.setChangeCode("CHG-1");
            change.setTitle("demo");
            change.setProjectBranch("release/20260708");
            change.setReleaseType(releaseType);
            change.setTargetEnvCode("prod");
            change.setTargetColor("green");
            change.setStatus(ChangeStatus.DRAFT);
            change.setDeveloperUsername("reviewer_a");
            change.setDeveloperDisplayName("评审 A");
            change.setReviewMode("TWO_REVIEWERS_PLUS_JUEGE");
            change.setDemoRequired(true);
            change.setDemoConfirmed(false);
            change.setRiskLevel("HIGH");
            change.setSummary("demo");
            change.setContentJson("{}");
            change.setCurrentStep("draft");
            change.setFinalMessage("draft");
            return change;
        }

        private Environment environment() {
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
            return env;
        }

        private Environment testEnvironment() {
            Environment env = new Environment();
            env.setEnvCode("test");
            env.setEnvName("测试环境");
            env.setEnvKind("TEST");
            env.setBaseUrl("https://juegeresource.top/");
            env.setCurrentColor("single");
            env.setSupportsBlueGreen(false);
            env.setAnnounceFileExists(false);
            env.setHealthStatus("READ_ONLY_OK");
            env.setNotes("test");
            return env;
        }

        private List<ReleaseChangeItem> items(Long changeId) {
            List<ReleaseChangeItem> result = new ArrayList<ReleaseChangeItem>();
            result.add(item(changeId, 11L, "mysql"));
            result.add(item(changeId, 12L, "redis"));
            return result;
        }

        private ReleaseChangeItem item(Long changeId, Long id, String componentKey) {
            ReleaseChangeItem item = new ReleaseChangeItem();
            item.setId(id);
            item.setChangeId(changeId);
            item.setItemKey("CHG-1-" + componentKey);
            item.setItemOrder("mysql".equals(componentKey) ? 1 : 2);
            item.setComponentKey(componentKey);
            item.setComponentName(componentKey.toUpperCase());
            item.setComponentType("DATABASE");
            item.setOwnerUsername("reviewer_a");
            item.setOwnerDisplayName("评审 A");
            item.setTitle(componentKey.toUpperCase());
            item.setChangeContent("content");
            item.setIncrementalPlan("incremental");
            item.setRollbackPlan("rollback");
            item.setTestPlan("test");
            item.setDataProbePlan("probe");
            item.setSpecStatus("READY");
            item.setLifecycleStatus("DRAFT");
            item.setReviewerAConfirmed(false);
            item.setReviewerBConfirmed(false);
            item.setJuegeConfirmed(false);
            return item;
        }

        private ComponentDefinition component(String key) {
            ComponentDefinition component = new ComponentDefinition();
            component.setComponentKey(key);
            component.setComponentName(key.toUpperCase());
            component.setComponentType("DATABASE");
            component.setConfigDir("/data/osh/config/" + key);
            component.setDataDir("/data/osh/data/" + key);
            component.setDeployPath("/data/osh/compose/" + key);
            component.setSupportIncremental(true);
            component.setSupportRollback(true);
            component.setSupportBlueGreen(true);
            component.setExtension(false);
            component.setCore(true);
            component.setInstallOrder("mysql".equals(key) ? 10 : 20);
            component.setRollbackOrder("mysql".equals(key) ? 90 : 80);
            component.setNotes(key);
            return component;
        }
    }

    private static class MapResult {
        private final java.util.Map<String, Object> values;

        MapResult(java.util.Map<String, Object> values) {
            this.values = values;
        }

        boolean booleanValue(String key) {
            return Boolean.TRUE.equals(values.get(key));
        }
    }
}
