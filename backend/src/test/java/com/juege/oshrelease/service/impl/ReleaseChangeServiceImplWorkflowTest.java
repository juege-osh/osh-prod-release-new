package com.juege.oshrelease.service.impl;

import com.juege.oshrelease.common.BusinessException;
import com.juege.oshrelease.common.ChangeStatus;
import com.juege.oshrelease.common.ReleaseType;
import com.juege.oshrelease.dto.ReleaseChangeCreateRequest;
import com.juege.oshrelease.dto.ReleaseChangeDetailDTO;
import com.juege.oshrelease.dto.ReleaseChangeItemDTO;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ReleaseChangeServiceImplWorkflowTest {

    @Test
    void fullWorkflowShouldCoverReviewDemoTestsBlueGreenAndRollback() {
        Fixture fixture = new Fixture();
        ReleaseChangeServiceImpl service = fixture.buildService();

        ReleaseChangeCreateRequest createRequest = new ReleaseChangeCreateRequest();
        createRequest.title = "完整上线演练";
        createRequest.projectBranch = "release/20260708";
        createRequest.releaseType = ReleaseType.NORMAL;
        createRequest.targetEnvCode = "prod";
        createRequest.targetColor = "green";
        createRequest.developerUsername = "reviewer_a";
        createRequest.developerDisplayName = "评审 A";
        createRequest.demoRequired = true;
        createRequest.riskLevel = "HIGH";
        createRequest.summary = "先上绿系统，再做人工验证，最后同步蓝系统。";
        createRequest.componentKeys = fixture.componentKeys();
        createRequest.contentJson = "{\"protectedModules\":[\"course\",\"user\"],\"prodWrite\":\"manual-confirm-required\"}";

        ReleaseChangeDetailDTO created = service.create(createRequest);
        assertEquals(ChangeStatus.DRAFT.name(), created.status);
        assertEquals(11, created.nodes.size());
        assertEquals(11, created.items.size());

        service.submit(created.id);

        ReleaseChangeOperationRequest approveA = reviewer("reviewer_a", "评审 A", "评审 A 确认通过");
        assertThrows(BusinessException.class, () -> service.approve(created.id, approveA));

        service.demo(created.id, demo("reviewer_a", "评审 A", "reviewer_b", "评审 B"));
        ReleaseChangeDetailDTO afterDemo = service.detail(created.id);
        assertTrue(afterDemo.demoConfirmed);
        assertNotNull(afterDemo.demos);
        assertEquals(1, afterDemo.demos.size());
        assertEquals("reviewer_a", afterDemo.demos.get(0).developerUsername);
        assertEquals("reviewer_b", afterDemo.demos.get(0).reviewerUsername);

        service.approve(created.id, approveA);
        service.approve(created.id, reviewer("reviewer_b", "评审 B", "评审 B 确认通过"));
        ReleaseChangeDetailDTO afterTeamReview = service.detail(created.id);
        assertEquals("REVIEWING", afterTeamReview.status);
        assertEquals("reviewer_a", afterTeamReview.reviewerAUsername);
        assertEquals("reviewer_b", afterTeamReview.reviewerBUsername);

        service.approve(created.id, reviewer("juege", "觉哥", "觉哥最终确认"));
        ReleaseChangeDetailDTO afterFinalApproval = service.detail(created.id);
        assertEquals(ChangeStatus.APPROVED.name(), afterFinalApproval.status);

        service.validateSpecs(created.id);
        ReleaseChangeDetailDTO afterSpec = service.detail(created.id);
        assertEquals(1, afterSpec.reports.size());
        assertEquals("SPEC", afterSpec.reports.get(0).reportType);

        for (ReleaseChangeItemDTO item : afterSpec.items) {
            service.reviewerTest(created.id, reviewerEvidence(item.id, item.componentKey, "reviewer_a", "评审 A", "test",
                    item.componentName + " 已在测试环境完成功能和回滚口径验证。"));
            service.reviewerTest(created.id, reviewerEvidence(item.id, item.componentKey, "reviewer_b", "评审 B", "prod-green",
                    item.componentName + " 已在绿环境复核功能和数据影响。"));
        }

        ReleaseChangeDetailDTO afterEvidence = service.detail(created.id);
        assertEquals(22, afterEvidence.evidences.size());

        service.functionTest(created.id);
        ReleaseChangeDetailDTO afterFunction = service.detail(created.id);
        assertTrue(afterFunction.operations.stream().anyMatch(operation -> "AUTO_FUNCTION_TEST".equals(operation.operationType)));
        assertEquals("TESTING", afterFunction.status);

        service.dataTest(created.id);
        service.envDiff(created.id);
        service.announceCheck(created.id);
        Map<String, Object> reports = service.reports(created.id);
        assertEquals(Boolean.TRUE, reports.get("specPassed"));
        assertEquals(Boolean.TRUE, reports.get("functionalPassed"));
        assertEquals(Boolean.TRUE, reports.get("dataPassed"));
        assertEquals(Boolean.TRUE, reports.get("envDiffGenerated"));
        assertEquals(Boolean.TRUE, reports.get("announceChecked"));
        assertTrue(String.valueOf(reports.get("prodSafety")).contains("安全模式"));
        assertTrue(fixture.testEnvironment().isAnnounceFileExists());

        service.switchGreen(created.id);
        ReleaseChangeDetailDTO afterSwitchGreen = service.detail(created.id);
        assertEquals(ChangeStatus.SWITCHED.name(), afterSwitchGreen.status);
        assertEquals("green", fixture.productionEnvironment().getCurrentColor());

        service.manualVerify(created.id, operator("ops", "运维同学", "绿系统人工验证通过"));
        ReleaseChangeDetailDTO afterManualVerify = service.detail(created.id);
        assertEquals(ChangeStatus.VERIFIED.name(), afterManualVerify.status);

        service.syncGreenToBlue(created.id, operator("ops", "运维同学", "同步蓝系统"));
        ReleaseChangeDetailDTO afterSyncBlue = service.detail(created.id);
        assertEquals(ChangeStatus.RELEASED.name(), afterSyncBlue.status);

        service.switchBlue(created.id);
        ReleaseChangeDetailDTO afterSwitchBlue = service.detail(created.id);
        assertEquals(ChangeStatus.ROLLED_BACK.name(), afterSwitchBlue.status);
        assertEquals("blue", fixture.productionEnvironment().getCurrentColor());

        service.rollback(created.id);
        ReleaseChangeDetailDTO afterRollback = service.detail(created.id);
        assertEquals(ChangeStatus.ROLLED_BACK.name(), afterRollback.status);
        assertTrue(afterRollback.operations.stream().anyMatch(operation -> "NODE_ROLLBACK".equals(operation.operationType)));
        for (ReleaseChangeItemDTO item : afterRollback.items) {
            assertEquals("ROLLED_BACK", item.lifecycleStatus);
        }
    }

    @Test
    void urgentWorkflowShouldRequireJuegeEvidenceBeforeFunctionTest() {
        Fixture fixture = new Fixture();
        ReleaseChangeServiceImpl service = fixture.buildService();

        ReleaseChangeCreateRequest createRequest = new ReleaseChangeCreateRequest();
        createRequest.title = "紧急上线演练";
        createRequest.projectBranch = "release/20260708";
        createRequest.releaseType = ReleaseType.URGENT;
        createRequest.targetEnvCode = "prod";
        createRequest.targetColor = "green";
        createRequest.developerUsername = "ops";
        createRequest.developerDisplayName = "运维同学";
        createRequest.demoRequired = false;
        createRequest.riskLevel = "CRITICAL";
        createRequest.summary = "紧急上线先走觉哥确认，再补齐人工测试证据。";
        createRequest.componentKeys = fixture.componentKeys();
        createRequest.contentJson = "{\"mode\":\"urgent\",\"protectModules\":[\"course\",\"user\"]}";

        ReleaseChangeDetailDTO created = service.create(createRequest);
        service.submit(created.id);
        service.approve(created.id, reviewer("reviewer_a", "评审 A", "评审 A 确认通过"));
        service.approve(created.id, reviewer("reviewer_b", "评审 B", "评审 B 确认通过"));
        service.approve(created.id, reviewer("juege", "觉哥", "觉哥最终确认"));
        service.validateSpecs(created.id);

        ReleaseChangeDetailDTO readyForFunction = service.detail(created.id);
        for (ReleaseChangeItemDTO item : readyForFunction.items) {
            service.reviewerTest(created.id, reviewerEvidence(item.id, item.componentKey, "reviewer_a", "评审 A", "test",
                    item.componentName + " 已在测试环境完成功能验证。"));
            service.reviewerTest(created.id, reviewerEvidence(item.id, item.componentKey, "reviewer_b", "评审 B", "prod-green",
                    item.componentName + " 已在绿环境复核。"));
        }

        assertThrows(BusinessException.class, () -> service.functionTest(created.id));

        for (ReleaseChangeItemDTO item : readyForFunction.items) {
            service.reviewerTest(created.id, reviewerEvidence(item.id, item.componentKey, "juege", "觉哥", "prod-green",
                    item.componentName + " 已补齐觉哥确认。"));
        }

        service.functionTest(created.id);
        service.dataTest(created.id);

        ReleaseChangeDetailDTO afterFunction = service.detail(created.id);
        assertEquals("TESTING", afterFunction.status);
        assertTrue(afterFunction.operations.stream().anyMatch(operation -> "AUTO_FUNCTION_TEST".equals(operation.operationType)));
        assertTrue(afterFunction.operations.stream().anyMatch(operation -> "AUTO_DATA_DIFF".equals(operation.operationType)));
        assertTrue(afterFunction.items.stream().allMatch(item -> item.juegeConfirmed));
    }

    private ReleaseChangeOperationRequest reviewer(String username, String displayName, String comment) {
        ReleaseChangeOperationRequest request = new ReleaseChangeOperationRequest();
        request.reviewerUsername = username;
        request.reviewerDisplayName = displayName;
        request.actorUsername = username;
        request.actorDisplayName = displayName;
        request.comment = comment;
        return request;
    }

    private ReleaseChangeOperationRequest demo(String actorUsername,
                                               String actorDisplayName,
                                               String reviewerUsername,
                                               String reviewerDisplayName) {
        ReleaseChangeOperationRequest request = new ReleaseChangeOperationRequest();
        request.actorUsername = actorUsername;
        request.actorDisplayName = actorDisplayName;
        request.reviewerUsername = reviewerUsername;
        request.reviewerDisplayName = reviewerDisplayName;
        request.comment = actorDisplayName + " 已向 " + reviewerDisplayName + " 演示上线内容。";
        return request;
    }

    private ReleaseChangeOperationRequest operator(String actorUsername, String actorDisplayName, String comment) {
        ReleaseChangeOperationRequest request = new ReleaseChangeOperationRequest();
        request.reviewerUsername = actorUsername;
        request.reviewerDisplayName = actorDisplayName;
        request.actorUsername = actorUsername;
        request.actorDisplayName = actorDisplayName;
        request.comment = comment;
        return request;
    }

    private ReviewerTestEvidenceRequest reviewerEvidence(Long itemId,
                                                         String componentKey,
                                                         String reviewerUsername,
                                                         String reviewerDisplayName,
                                                         String environmentCode,
                                                         String evidence) {
        ReviewerTestEvidenceRequest request = new ReviewerTestEvidenceRequest();
        request.itemId = itemId;
        request.componentKey = componentKey;
        request.reviewerUsername = reviewerUsername;
        request.reviewerDisplayName = reviewerDisplayName;
        request.testType = "FUNCTION";
        request.environmentCode = environmentCode;
        request.passed = true;
        request.demoObserved = true;
        request.responsibilityAccepted = true;
        request.evidence = evidence;
        return request;
    }

    private static final class Fixture {
        private final ReleaseChangeRepository releaseChangeRepository = Mockito.mock(ReleaseChangeRepository.class);
        private final ReleaseNodeRepository releaseNodeRepository = Mockito.mock(ReleaseNodeRepository.class);
        private final ReleaseChangeItemRepository releaseChangeItemRepository = Mockito.mock(ReleaseChangeItemRepository.class);
        private final ReviewRecordRepository reviewRecordRepository = Mockito.mock(ReviewRecordRepository.class);
        private final ReviewerTestEvidenceRepository reviewerTestEvidenceRepository = Mockito.mock(ReviewerTestEvidenceRepository.class);
        private final DemoRecordRepository demoRecordRepository = Mockito.mock(DemoRecordRepository.class);
        private final TestReportRepository testReportRepository = Mockito.mock(TestReportRepository.class);
        private final ReleaseOperationRecordRepository releaseOperationRecordRepository = Mockito.mock(ReleaseOperationRecordRepository.class);
        private final ComponentRepository componentRepository = Mockito.mock(ComponentRepository.class);
        private final EnvironmentRepository environmentRepository = Mockito.mock(EnvironmentRepository.class);
        @SuppressWarnings("unused")
        private final PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);

        private final List<ReleaseChange> changes = new ArrayList<ReleaseChange>();
        private final List<ReleaseNode> nodes = new ArrayList<ReleaseNode>();
        private final List<ReleaseChangeItem> items = new ArrayList<ReleaseChangeItem>();
        private final List<ReviewRecord> reviews = new ArrayList<ReviewRecord>();
        private final List<ReviewerTestEvidence> evidences = new ArrayList<ReviewerTestEvidence>();
        private final List<DemoRecord> demos = new ArrayList<DemoRecord>();
        private final List<TestReport> reports = new ArrayList<TestReport>();
        private final List<ReleaseOperationRecord> operations = new ArrayList<ReleaseOperationRecord>();
        private final Map<String, ComponentDefinition> components = new LinkedHashMap<String, ComponentDefinition>();
        private final Map<String, Environment> environments = new LinkedHashMap<String, Environment>();
        private final AtomicLong changeIds = new AtomicLong(0L);
        private final AtomicLong nodeIds = new AtomicLong(0L);
        private final AtomicLong itemIds = new AtomicLong(0L);
        private final AtomicLong reviewIds = new AtomicLong(0L);
        private final AtomicLong evidenceIds = new AtomicLong(0L);
        private final AtomicLong demoIds = new AtomicLong(0L);
        private final AtomicLong reportIds = new AtomicLong(0L);
        private final AtomicLong operationIds = new AtomicLong(0L);

        Fixture() {
            seedEnvironments();
            seedComponents();
            stubRepositories();
        }

        ReleaseChangeServiceImpl buildService() {
            return new ReleaseChangeServiceImpl(
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
        }

        List<String> componentKeys() {
            return Arrays.asList(
                    "mysql",
                    "redis",
                    "nacos",
                    "kafka",
                    "elasticsearch",
                    "hbase",
                    "java-backend",
                    "vue-frontend",
                    "nginx",
                    "docker-compose",
                    "mongodb"
            );
        }

        Environment testEnvironment() {
            return environments.get("test");
        }

        Environment productionEnvironment() {
            return environments.get("prod");
        }

        private void seedEnvironments() {
            environments.put("test", environment("test", "测试环境", "TEST", "https://juegeresource.top/", null, null, "single", false, false, "READ_ONLY_OK", "测试服只读探测通过。"));
            environments.put("prod", environment("prod", "生产环境", "PROD", "https://osh.lol/", "https://osh.lol/", "https://osh.lol/", "blue", true, false, "PROTECTED", "生产服默认只读保护。"));
            environments.put("prod-green", environment("prod-green", "生产绿环境", "PROD_GREEN", "https://osh.lol/", "https://osh.lol/", "https://osh.lol/", "green", true, false, "STANDBY", "绿环境用于先发布、先验证。"));
        }

        private void seedComponents() {
            putComponent(component("mysql", "MySQL", "DATABASE", "/data/osh/config/mysql", "/data/osh/data/mysql", "/data/osh/compose/mysql", true, true, true, false, true, 10, 90, "MySQL"));
            putComponent(component("redis", "Redis", "CACHE", "/data/osh/config/redis", "/data/osh/data/redis", "/data/osh/compose/redis", true, true, true, false, true, 20, 80, "Redis"));
            putComponent(component("nacos", "Nacos", "CONFIG", "/data/osh/config/nacos", "/data/osh/data/nacos", "/data/osh/compose/nacos", true, true, true, false, true, 30, 70, "Nacos"));
            putComponent(component("kafka", "Kafka", "MESSAGE", "/data/osh/config/kafka", "/data/osh/data/kafka", "/data/osh/compose/kafka", true, true, true, false, true, 40, 60, "Kafka"));
            putComponent(component("elasticsearch", "Elasticsearch", "SEARCH", "/data/osh/config/es", "/data/osh/data/es", "/data/osh/compose/es", true, true, true, false, true, 50, 50, "Elasticsearch"));
            putComponent(component("hbase", "HBase", "STORAGE", "/data/osh/config/hbase", "/data/osh/data/hbase", "/data/osh/compose/hbase", true, true, true, false, true, 60, 40, "HBase"));
            putComponent(component("java-backend", "Java 后端服务", "APP", "/data/osh/config/backend", "/data/osh/apps/backend", "/data/osh/compose/backend", true, true, true, false, true, 70, 30, "Java 后端"));
            putComponent(component("vue-frontend", "Vue 前端", "WEB", "/data/osh/config/frontend", "/data/osh/apps/frontend", "/data/osh/compose/frontend", true, true, true, false, true, 80, 20, "Vue 前端"));
            putComponent(component("nginx", "Nginx 网关", "GATEWAY", "/data/osh/config/nginx", "/data/osh/data/nginx", "/data/osh/compose/nginx", true, true, true, false, true, 90, 10, "Nginx"));
            putComponent(component("docker-compose", "Docker Compose 编排", "ORCHESTRATION", "/data/osh/config/compose", "/data/osh/data/compose", "/data/osh/compose", true, true, true, false, true, 100, 5, "Compose"));
            putComponent(component("mongodb", "MongoDB 扩展组件", "DATABASE", "/data/osh/config/mongodb", "/data/osh/data/mongodb", "/data/osh/compose/mongodb", true, true, true, true, false, 110, 1, "MongoDB"));
        }

        private void putComponent(ComponentDefinition component) {
            components.put(component.getComponentKey(), component);
        }

        private void stubRepositories() {
            when(environmentRepository.findByEnvCode(anyString())).thenAnswer(invocation -> Optional.ofNullable(environments.get(invocation.getArgument(0))));
            when(componentRepository.findByComponentKey(anyString())).thenAnswer(invocation -> Optional.ofNullable(components.get(invocation.getArgument(0))));
            when(componentRepository.findAll()).thenAnswer(invocation -> new ArrayList<ComponentDefinition>(components.values()));

            when(releaseChangeRepository.save(any(ReleaseChange.class))).thenAnswer(invocation -> storeChange(invocation.getArgument(0)));
            when(releaseChangeRepository.findById(any())).thenAnswer(invocation -> findById(changes, invocation.getArgument(0)));
            when(releaseChangeRepository.findAll()).thenAnswer(invocation -> new ArrayList<ReleaseChange>(changes));

            when(releaseNodeRepository.save(any(ReleaseNode.class))).thenAnswer(invocation -> storeNode(invocation.getArgument(0)));
            when(releaseNodeRepository.findByChangeIdOrderByNodeOrderAsc(any())).thenAnswer(invocation -> filterNodes(invocation.getArgument(0)));

            when(releaseChangeItemRepository.save(any(ReleaseChangeItem.class))).thenAnswer(invocation -> storeItem(invocation.getArgument(0)));
            when(releaseChangeItemRepository.findByChangeIdOrderByItemOrderAsc(any())).thenAnswer(invocation -> filterItems(invocation.getArgument(0)));
            when(releaseChangeItemRepository.findById(any())).thenAnswer(invocation -> findById(items, invocation.getArgument(0)));

            when(reviewRecordRepository.save(any(ReviewRecord.class))).thenAnswer(invocation -> storeReview(invocation.getArgument(0)));
            when(reviewRecordRepository.findByChangeIdOrderByCreatedAtAsc(any())).thenAnswer(invocation -> filterReviews(invocation.getArgument(0)));

            when(reviewerTestEvidenceRepository.save(any(ReviewerTestEvidence.class))).thenAnswer(invocation -> storeEvidence(invocation.getArgument(0)));
            when(reviewerTestEvidenceRepository.findByChangeIdOrderByCreatedAtAsc(any())).thenAnswer(invocation -> filterEvidences(invocation.getArgument(0)));

            when(demoRecordRepository.save(any(DemoRecord.class))).thenAnswer(invocation -> storeDemo(invocation.getArgument(0)));
            when(demoRecordRepository.findByChangeIdOrderByCreatedAtAsc(any())).thenAnswer(invocation -> filterDemos(invocation.getArgument(0)));

            when(testReportRepository.save(any(TestReport.class))).thenAnswer(invocation -> storeReport(invocation.getArgument(0)));
            when(testReportRepository.findByChangeIdOrderByCreatedAtAsc(any())).thenAnswer(invocation -> filterReports(invocation.getArgument(0)));

            when(releaseOperationRecordRepository.save(any(ReleaseOperationRecord.class))).thenAnswer(invocation -> storeOperation(invocation.getArgument(0)));
            when(releaseOperationRecordRepository.findByChangeIdOrderByCreatedAtAsc(any())).thenAnswer(invocation -> filterOperations(invocation.getArgument(0)));
        }

        private ReleaseChange storeChange(ReleaseChange change) {
            return upsert(changes, change, changeIds);
        }

        private ReleaseNode storeNode(ReleaseNode node) {
            return upsert(nodes, node, nodeIds);
        }

        private ReleaseChangeItem storeItem(ReleaseChangeItem item) {
            return upsert(items, item, itemIds);
        }

        private ReviewRecord storeReview(ReviewRecord review) {
            return upsert(reviews, review, reviewIds);
        }

        private ReviewerTestEvidence storeEvidence(ReviewerTestEvidence evidence) {
            return upsert(evidences, evidence, evidenceIds);
        }

        private DemoRecord storeDemo(DemoRecord demoRecord) {
            return upsert(demos, demoRecord, demoIds);
        }

        private TestReport storeReport(TestReport report) {
            return upsert(reports, report, reportIds);
        }

        private ReleaseOperationRecord storeOperation(ReleaseOperationRecord operation) {
            return upsert(operations, operation, operationIds);
        }

        private <T extends com.juege.oshrelease.model.BaseEntity> T upsert(List<T> store, T entity, AtomicLong counter) {
            if (entity.getId() == null) {
                entity.setId(counter.incrementAndGet());
            }
            LocalDateTime now = LocalDateTime.now();
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(now);
            }
            entity.setUpdatedAt(now);
            for (int i = 0; i < store.size(); i++) {
                if (store.get(i).getId().equals(entity.getId())) {
                    store.set(i, entity);
                    return entity;
                }
            }
            store.add(entity);
            return entity;
        }

        private <T extends com.juege.oshrelease.model.BaseEntity> Optional<T> findById(List<T> store, Long id) {
            if (id == null) {
                return Optional.empty();
            }
            for (T entity : store) {
                if (id.equals(entity.getId())) {
                    return Optional.of(entity);
                }
            }
            return Optional.empty();
        }

        private List<ReleaseNode> filterNodes(Long changeId) {
            List<ReleaseNode> result = new ArrayList<ReleaseNode>();
            for (ReleaseNode node : nodes) {
                if (changeId.equals(node.getChangeId())) {
                    result.add(node);
                }
            }
            Collections.sort(result, Comparator.comparingInt(ReleaseNode::getNodeOrder));
            return result;
        }

        private List<ReleaseChangeItem> filterItems(Long changeId) {
            List<ReleaseChangeItem> result = new ArrayList<ReleaseChangeItem>();
            for (ReleaseChangeItem item : items) {
                if (changeId.equals(item.getChangeId())) {
                    result.add(item);
                }
            }
            Collections.sort(result, Comparator.comparingInt(ReleaseChangeItem::getItemOrder));
            return result;
        }

        private List<ReviewRecord> filterReviews(Long changeId) {
            List<ReviewRecord> result = new ArrayList<ReviewRecord>();
            for (ReviewRecord review : reviews) {
                if (changeId.equals(review.getChangeId())) {
                    result.add(review);
                }
            }
            return result;
        }

        private List<ReviewerTestEvidence> filterEvidences(Long changeId) {
            List<ReviewerTestEvidence> result = new ArrayList<ReviewerTestEvidence>();
            for (ReviewerTestEvidence evidence : evidences) {
                if (changeId.equals(evidence.getChangeId())) {
                    result.add(evidence);
                }
            }
            return result;
        }

        private List<DemoRecord> filterDemos(Long changeId) {
            List<DemoRecord> result = new ArrayList<DemoRecord>();
            for (DemoRecord demoRecord : demos) {
                if (changeId.equals(demoRecord.getChangeId())) {
                    result.add(demoRecord);
                }
            }
            return result;
        }

        private List<TestReport> filterReports(Long changeId) {
            List<TestReport> result = new ArrayList<TestReport>();
            for (TestReport report : reports) {
                if (changeId.equals(report.getChangeId())) {
                    result.add(report);
                }
            }
            return result;
        }

        private List<ReleaseOperationRecord> filterOperations(Long changeId) {
            List<ReleaseOperationRecord> result = new ArrayList<ReleaseOperationRecord>();
            for (ReleaseOperationRecord operation : operations) {
                if (changeId.equals(operation.getChangeId())) {
                    result.add(operation);
                }
            }
            return result;
        }

        private ComponentDefinition component(String key, String name, String type, String configDir, String dataDir, String deployPath,
                                              boolean incremental, boolean rollback, boolean blueGreen, boolean extension, boolean core,
                                              int installOrder, int rollbackOrder, String notes) {
            ComponentDefinition component = new ComponentDefinition();
            component.setComponentKey(key);
            component.setComponentName(name);
            component.setComponentType(type);
            component.setConfigDir(configDir);
            component.setDataDir(dataDir);
            component.setDeployPath(deployPath);
            component.setSupportIncremental(incremental);
            component.setSupportRollback(rollback);
            component.setSupportBlueGreen(blueGreen);
            component.setExtension(extension);
            component.setCore(core);
            component.setInstallOrder(installOrder);
            component.setRollbackOrder(rollbackOrder);
            component.setNotes(notes);
            return component;
        }

        private Environment environment(String code, String name, String kind, String baseUrl, String blueUrl, String greenUrl,
                                        String currentColor, boolean supportsBlueGreen, boolean announceFileExists,
                                        String healthStatus, String notes) {
            Environment environment = new Environment();
            environment.setEnvCode(code);
            environment.setEnvName(name);
            environment.setEnvKind(kind);
            environment.setBaseUrl(baseUrl);
            environment.setBlueUrl(blueUrl);
            environment.setGreenUrl(greenUrl);
            environment.setCurrentColor(currentColor);
            environment.setSupportsBlueGreen(supportsBlueGreen);
            environment.setAnnounceFileExists(announceFileExists);
            environment.setHealthStatus(healthStatus);
            environment.setNotes(notes);
            return environment;
        }
    }
}
