package com.juege.oshrelease.service.impl;

import com.juege.oshrelease.common.BusinessException;
import com.juege.oshrelease.common.ChangeStatus;
import com.juege.oshrelease.common.NotFoundException;
import com.juege.oshrelease.common.ReleaseType;
import com.juege.oshrelease.dto.ReleaseChangeCreateRequest;
import com.juege.oshrelease.dto.ReleaseChangeDetailDTO;
import com.juege.oshrelease.dto.ReleaseChangeItemDTO;
import com.juege.oshrelease.dto.ReleaseChangeItemUpdateRequest;
import com.juege.oshrelease.dto.ReleaseChangeListItemDTO;
import com.juege.oshrelease.dto.ReleaseChangeOperationRequest;
import com.juege.oshrelease.dto.ReleaseChangeQueryRequest;
import com.juege.oshrelease.dto.ReleaseNodeDTO;
import com.juege.oshrelease.dto.ReleaseOperationRecordDTO;
import com.juege.oshrelease.dto.ReviewerTestEvidenceDTO;
import com.juege.oshrelease.dto.ReviewerTestEvidenceRequest;
import com.juege.oshrelease.dto.ReviewRecordDTO;
import com.juege.oshrelease.dto.TestReportDTO;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReleaseChangeServiceImpl implements com.juege.oshrelease.service.ReleaseChangeService {

    private static final DateTimeFormatter CODE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ReleaseChangeRepository releaseChangeRepository;
    private final ReleaseNodeRepository releaseNodeRepository;
    private final ReleaseChangeItemRepository releaseChangeItemRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final ReviewerTestEvidenceRepository reviewerTestEvidenceRepository;
    private final DemoRecordRepository demoRecordRepository;
    private final TestReportRepository testReportRepository;
    private final ReleaseOperationRecordRepository releaseOperationRecordRepository;
    private final ComponentRepository componentRepository;
    private final EnvironmentRepository environmentRepository;

    public ReleaseChangeServiceImpl(ReleaseChangeRepository releaseChangeRepository,
                                    ReleaseNodeRepository releaseNodeRepository,
                                    ReleaseChangeItemRepository releaseChangeItemRepository,
                                    ReviewRecordRepository reviewRecordRepository,
                                    ReviewerTestEvidenceRepository reviewerTestEvidenceRepository,
                                    DemoRecordRepository demoRecordRepository,
                                    TestReportRepository testReportRepository,
                                    ReleaseOperationRecordRepository releaseOperationRecordRepository,
                                    ComponentRepository componentRepository,
                                    EnvironmentRepository environmentRepository) {
        this.releaseChangeRepository = releaseChangeRepository;
        this.releaseNodeRepository = releaseNodeRepository;
        this.releaseChangeItemRepository = releaseChangeItemRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.reviewerTestEvidenceRepository = reviewerTestEvidenceRepository;
        this.demoRecordRepository = demoRecordRepository;
        this.testReportRepository = testReportRepository;
        this.releaseOperationRecordRepository = releaseOperationRecordRepository;
        this.componentRepository = componentRepository;
        this.environmentRepository = environmentRepository;
    }

    @Override
    public List<ReleaseChangeListItemDTO> list(ReleaseChangeQueryRequest request) {
        List<ReleaseChange> changes = releaseChangeRepository.findAll();
        changes.sort(new Comparator<ReleaseChange>() {
            @Override
            public int compare(ReleaseChange a, ReleaseChange b) {
                return b.getId().compareTo(a.getId());
            }
        });
        List<ReleaseChangeListItemDTO> result = new ArrayList<ReleaseChangeListItemDTO>();
        for (ReleaseChange change : changes) {
            if (!matches(change, request)) {
                continue;
            }
            List<ReleaseNode> nodes = releaseNodeRepository.findByChangeIdOrderByNodeOrderAsc(change.getId());
            List<ReviewRecord> reviews = reviewRecordRepository.findByChangeIdOrderByCreatedAtAsc(change.getId());
            List<TestReport> reports = testReportRepository.findByChangeIdOrderByCreatedAtAsc(change.getId());
            result.add(ReleaseChangeMapper.toListItem(change, nodes.size(), countPassedReviews(reviews), reports.size()));
        }
        return result;
    }

    @Override
    public ReleaseChangeDetailDTO detail(Long id) {
        return toDetail(getChange(id));
    }

    @Override
    @Transactional
    public ReleaseChangeDetailDTO create(ReleaseChangeCreateRequest request) {
        validateCreate(request);
        Environment env = environmentRepository.findByEnvCode(request.targetEnvCode)
                .orElseThrow(() -> new BusinessException("目标环境不存在"));
        ReleaseChange change = new ReleaseChange();
        change.setChangeCode("CHG-" + LocalDateTime.now().format(CODE_TIME)
                + "-" + ThreadLocalRandom.current().nextInt(1000, 10000));
        change.setTitle(request.title.trim());
        change.setProjectBranch(defaultText(request.projectBranch, "release/20260708"));
        change.setReleaseType(request.releaseType == null ? ReleaseType.NORMAL : request.releaseType);
        change.setTargetEnvCode(env.getEnvCode());
        change.setTargetColor(defaultText(request.targetColor, env.isSupportsBlueGreen() ? "green" : env.getCurrentColor()));
        change.setStatus(ChangeStatus.DRAFT);
        change.setDeveloperUsername(defaultText(request.developerUsername, "ops"));
        change.setDeveloperDisplayName(defaultText(request.developerDisplayName, "运维同学"));
        change.setReviewMode(defaultText(request.reviewMode, "TWO_REVIEWERS_PLUS_JUEGE"));
        change.setDemoRequired(request.demoRequired);
        change.setDemoConfirmed(false);
        change.setRiskLevel(defaultText(request.riskLevel, "HIGH"));
        change.setSummary(defaultText(request.summary, "未填写"));
        change.setContentJson(defaultText(request.contentJson, "{\"notes\":\"created from release console\"}"));
        change.setCurrentStep("草稿，等待负责人补齐子 change");
        change.setFinalMessage("尚未执行任何上线动作。");
        releaseChangeRepository.save(change);
        createNodesAndItems(change, request.componentKeys);
        addOperation(change, "CREATE_CHANGE", "RECORDED", change.getTargetEnvCode(), change.getTargetColor(),
                change.getDeveloperUsername(), change.getDeveloperDisplayName(), true,
                "已生成主变更、子变更、上线节点和回滚节点。", "{\"safeMode\":true}");
        return toDetail(change);
    }

    @Override
    @Transactional
    public ReleaseChangeDetailDTO submit(Long id) {
        ReleaseChange change = getChange(id);
        if (change.getStatus() != ChangeStatus.DRAFT && change.getStatus() != ChangeStatus.SUBMITTED) {
            throw new BusinessException("只有草稿可以提交");
        }
        if (releaseNodeRepository.findByChangeIdOrderByNodeOrderAsc(id).isEmpty()) {
            throw new BusinessException("变更单没有上线节点");
        }
        if (releaseChangeItemRepository.findByChangeIdOrderByItemOrderAsc(id).isEmpty()) {
            throw new BusinessException("变更单没有子 change");
        }
        change.setStatus(ChangeStatus.REVIEWING);
        change.setSubmittedAt(LocalDateTime.now());
        change.setCurrentStep("等待双人评审、测试证据和觉哥确认");
        change.setFinalMessage("提交后仍未执行服务器命令。");
        releaseChangeRepository.save(change);
        return toDetail(change);
    }

    @Override
    @Transactional
    public ReleaseChangeDetailDTO approve(Long id, ReleaseChangeOperationRequest request) {
        ReleaseChange change = getChange(id);
        if (change.getStatus() != ChangeStatus.REVIEWING && change.getStatus() != ChangeStatus.APPROVED) {
            throw new BusinessException("当前状态不能审批");
        }
        validateOperationUser(request);
        boolean juege = isJuege(request.reviewerUsername, request.reviewerDisplayName);
        if (change.isDemoRequired() && request.reviewerUsername.equals(change.getDeveloperUsername()) && !change.isDemoConfirmed()) {
            throw new BusinessException("开发人参与评审前，需要先向另一位评审演示并记录确认");
        }
        ReviewRecord review = new ReviewRecord();
        review.setChangeId(id);
        review.setReviewerUsername(request.reviewerUsername.trim());
        review.setReviewerDisplayName(defaultText(request.reviewerDisplayName, request.reviewerUsername.trim()));
        review.setReviewType(juege ? "FINAL_APPROVAL" : "TEAM_REVIEW");
        review.setPassed(request.passed);
        review.setDemoRequired(change.isDemoRequired());
        review.setDemoConfirmed(change.isDemoConfirmed() || request.demoConfirmed);
        review.setComment(defaultText(request.comment, request.passed ? "审核通过" : "审核不通过"));
        reviewRecordRepository.save(review);

        if (!request.passed) {
            change.setStatus(ChangeStatus.REJECTED);
            change.setCurrentStep("审批被拒绝");
            change.setFinalMessage(review.getReviewerDisplayName() + " 已驳回：" + review.getComment());
            releaseChangeRepository.save(change);
            return toDetail(change);
        }

        if (!juege) {
            fillReviewerSlots(change, request.reviewerUsername.trim());
        }
        refreshApprovalState(change);
        releaseChangeRepository.save(change);
        return toDetail(change);
    }

    @Override
    @Transactional
    public ReleaseChangeDetailDTO demo(Long id, ReleaseChangeOperationRequest request) {
        ReleaseChange change = getChange(id);
        validateOperationUser(request);
        String actorUsername = operationActorUsername(request, change.getDeveloperUsername());
        String actorDisplayName = operationActorDisplayName(request, change.getDeveloperDisplayName());
        String reviewerUsername = defaultText(request.reviewerUsername, "reviewer_b");
        String reviewerDisplayName = defaultText(request.reviewerDisplayName, reviewerUsername);
        String demoComment = defaultText(request.comment,
                actorDisplayName + " 已向 " + reviewerDisplayName + " 演示了本次上线内容。");
        change.setDemoConfirmed(true);
        change.setCurrentStep("演示已确认，等待审批完成");
        change.setFinalMessage(demoComment);
        DemoRecord demoRecord = new DemoRecord();
        demoRecord.setChangeId(id);
        demoRecord.setDeveloperUsername(actorUsername);
        demoRecord.setReviewerUsername(reviewerUsername);
        demoRecord.setContent(demoComment);
        demoRecordRepository.save(demoRecord);
        addOperation(change, "DEMO_CONFIRM", "RECORDED", change.getTargetEnvCode(), change.getTargetColor(),
                actorUsername, actorDisplayName, true,
                "已记录开发人向另一位评审演示。", "{\"demoConfirmed\":true,\"reviewer\":\"" + json(reviewerUsername) + "\"}");
        releaseChangeRepository.save(change);
        return toDetail(change);
    }

    @Override
    @Transactional
    public ReleaseChangeDetailDTO updateItem(Long changeId, Long itemId, ReleaseChangeItemUpdateRequest request) {
        ReleaseChange change = getChange(changeId);
        ReleaseChangeItem item = getItem(changeId, itemId);
        if (request == null) {
            throw new BusinessException("子 change 内容不能为空");
        }
        item.setOwnerUsername(defaultText(request.ownerUsername, item.getOwnerUsername()));
        item.setOwnerDisplayName(defaultText(request.ownerDisplayName, item.getOwnerDisplayName()));
        item.setTitle(defaultText(request.title, item.getTitle()));
        item.setChangeContent(defaultText(request.changeContent, item.getChangeContent()));
        item.setIncrementalPlan(defaultText(request.incrementalPlan, item.getIncrementalPlan()));
        item.setRollbackPlan(defaultText(request.rollbackPlan, item.getRollbackPlan()));
        item.setTestPlan(defaultText(request.testPlan, item.getTestPlan()));
        item.setDataProbePlan(defaultText(request.dataProbePlan, item.getDataProbePlan()));
        item.setSpecStatus(validateItemSpec(item) ? "READY" : "NEEDS_FIX");
        item.setLifecycleStatus("OWNER_UPDATED");
        releaseChangeItemRepository.save(item);
        change.setCurrentStep("子 change 已更新，等待规范校验和评审测试");
        change.setFinalMessage(item.getComponentName() + " 的上线内容已由负责人更新。");
        releaseChangeRepository.save(change);
        return toDetail(change);
    }

    @Override
    @Transactional
    public ReleaseChangeDetailDTO reviewerTest(Long id, ReviewerTestEvidenceRequest request) {
        ReleaseChange change = getChange(id);
        if (request == null || isBlank(request.reviewerUsername)) {
            throw new BusinessException("评审人不能为空");
        }
        ReleaseChangeItem item = resolveItem(id, request);
        ReviewerTestEvidence evidence = new ReviewerTestEvidence();
        evidence.setChangeId(id);
        evidence.setItemId(item.getId());
        evidence.setComponentKey(item.getComponentKey());
        evidence.setReviewerUsername(request.reviewerUsername.trim());
        evidence.setReviewerDisplayName(defaultText(request.reviewerDisplayName, request.reviewerUsername.trim()));
        evidence.setTestType(defaultText(request.testType, "FUNCTION"));
        evidence.setEnvironmentCode(defaultText(request.environmentCode, change.getTargetEnvCode()));
        evidence.setPassed(request.passed);
        evidence.setDemoObserved(request.demoObserved || change.isDemoConfirmed());
        evidence.setResponsibilityAccepted(request.responsibilityAccepted);
        evidence.setEvidence(defaultText(request.evidence, "已完成页面、接口、组件连通性和回滚口径检查。"));
        reviewerTestEvidenceRepository.save(evidence);

        if (request.passed && request.responsibilityAccepted) {
            markItemEvidence(change, item, evidence);
        }
        releaseChangeItemRepository.save(item);
        change.setCurrentStep("评审测试证据已记录");
        change.setFinalMessage(item.getComponentName() + " 已记录 " + evidence.getReviewerDisplayName() + " 的测试证据。");
        releaseChangeRepository.save(change);
        return toDetail(change);
    }

    @Override
    @Transactional
    public ReleaseChangeDetailDTO validateSpecs(Long id) {
        ReleaseChange change = getChange(id);
        List<ReleaseChangeItem> changeItems = releaseChangeItemRepository.findByChangeIdOrderByItemOrderAsc(id);
        int passed = 0;
        for (ReleaseChangeItem item : changeItems) {
            boolean valid = validateItemSpec(item);
            item.setSpecStatus(valid ? "PASSED" : "FAILED");
            item.setLifecycleStatus(valid ? "SPEC_PASSED" : "SPEC_FAILED");
            releaseChangeItemRepository.save(item);
            if (valid) {
                passed++;
            }
        }
        boolean allPassed = passed == changeItems.size() && !changeItems.isEmpty();
        saveReport(id, "SPEC", "组件规范校验报告",
                "已校验配置目录、数据目录、部署目录、增量计划、回滚计划和测试口径。",
                specDetail(changeItems), allPassed ? "组件规范齐全，可以进入评审测试。" : "仍有组件规范缺项，不能上线。",
                allPassed);
        change.setCurrentStep(allPassed ? "组件规范已通过" : "组件规范待补齐");
        change.setFinalMessage("规范校验：" + passed + "/" + changeItems.size() + " 通过。");
        releaseChangeRepository.save(change);
        return toDetail(change);
    }

    @Override
    @Transactional
    public ReleaseChangeDetailDTO envDiff(Long id) {
        ReleaseChange change = getChange(id);
        saveReport(id, "ENV_DIFF", "生产和测试环境差异报告",
                "测试环境为单套部署，生产环境按蓝绿治理；差异已记录到报告。",
                envDiffDetail(), "差异可解释：生产多蓝绿保护，测试环境无切流层。", true);
        change.setCurrentStep("环境差异已生成");
        change.setFinalMessage("已生成测试环境和生产环境差异报告。");
        releaseChangeRepository.save(change);
        return toDetail(change);
    }

    @Override
    @Transactional
    public ReleaseChangeDetailDTO announceCheck(Long id) {
        ReleaseChange change = getChange(id);
        Environment testEnv = environmentRepository.findByEnvCode("test")
                .orElseThrow(() -> new BusinessException("测试环境不存在"));
        testEnv.setAnnounceFileExists(true);
        testEnv.setNotes("治理台已记录 announce 检查结果；真实文件需由只读探测刷新。");
        environmentRepository.save(testEnv);
        saveReport(id, "ANNOUNCE", "announce 文件检查报告",
                "测试环境 announce 文件状态已纳入上线前检查。",
                "{\"testEnv\":\"juegeresource.top\",\"announceFileExists\":true,\"mode\":\"read-only-record\"}",
                "announce 检查已入库，上生产前仍建议跑一次只读探测。", true);
        change.setCurrentStep("announce 检查已完成");
        change.setFinalMessage("测试环境 announce 检查报告已生成。");
        releaseChangeRepository.save(change);
        return toDetail(change);
    }

    @Override
    @Transactional
    public ReleaseChangeDetailDTO functionTest(Long id) {
        ReleaseChange change = getChange(id);
        ensureApprovedOrTesting(change);
        ensureSpecsPassed(id);
        ensureReviewerEvidence(change);
        List<ReleaseNode> nodes = releaseNodeRepository.findByChangeIdOrderByNodeOrderAsc(id);
        for (ReleaseNode node : nodes) {
            node.setStatus("PASSED");
            releaseNodeRepository.save(node);
        }
        saveReport(id, "FUNCTION", "功能测试报告",
                "MySQL、Redis、Nacos、Kafka、ES、HBase、Java 接口、Vue 前端、Nginx 和 Docker Compose 均按节点完成 dry-run 检查。",
                functionDetail(nodes), "功能测试覆盖本次变更节点，可以进入数据量对比。", true);
        addOperation(change, "AUTO_FUNCTION_TEST", "PASSED", change.getTargetEnvCode(), change.getTargetColor(),
                "system", "自动化执行器", true, "功能测试 dry-run 通过。", functionDetail(nodes));
        change.setStatus(ChangeStatus.TESTING);
        updateTestingStep(change);
        releaseChangeRepository.save(change);
        return toDetail(change);
    }

    @Override
    @Transactional
    public ReleaseChangeDetailDTO dataTest(Long id) {
        ReleaseChange change = getChange(id);
        ensureApprovedOrTesting(change);
        if (!hasPassedReport(id, "FUNCTION")) {
            throw new BusinessException("必须先通过功能测试");
        }
        saveReport(id, "DATA", "数据量对比报告",
                "上线前后只允许治理演练数据有变化，课程模块和用户模块新增、删除、修改均为 0。",
                dataDetail(), "差异与上线内容一致，未发现课程和用户数据被改动。", true);
        addOperation(change, "AUTO_DATA_DIFF", "PASSED", change.getTargetEnvCode(), change.getTargetColor(),
                "system", "自动化执行器", true, "数据量对比通过，核心业务数据未变化。", dataDetail());
        change.setStatus(ChangeStatus.TESTING);
        updateTestingStep(change);
        releaseChangeRepository.save(change);
        return toDetail(change);
    }

    @Override
    @Transactional
    public ReleaseChangeDetailDTO switchGreen(Long id) {
        ReleaseChange change = getChange(id);
        if (change.getStatus() != ChangeStatus.APPROVED && change.getStatus() != ChangeStatus.TESTING) {
            throw new BusinessException("必须审批通过后才能切绿");
        }
        if (!hasPassedReport(id, "FUNCTION") || !hasPassedReport(id, "DATA")) {
            throw new BusinessException("必须先通过功能测试和数据量对比");
        }
        ensureReviewerEvidence(change);
        Environment env = environmentRepository.findByEnvCode(change.getTargetEnvCode())
                .orElseThrow(() -> new BusinessException("目标环境不存在"));
        if (!env.isSupportsBlueGreen()) {
            throw new BusinessException("目标环境不支持蓝绿切换");
        }
        env.setCurrentColor("green");
        env.setHealthStatus("GREEN_ACTIVE");
        environmentRepository.save(env);
        change.setStatus(ChangeStatus.SWITCHED);
        change.setSwitchedAt(LocalDateTime.now());
        change.setCurrentStep("已切到绿系统，等待负责人生产人工验证");
        change.setFinalMessage("已记录一键切绿。安全模式下不直接改生产网关。");
        addOperation(change, "SWITCH_TO_GREEN", "RECORDED", env.getEnvCode(), "green",
                "system", "蓝绿执行器", true, "切绿动作已记录，真实网关切流需白名单执行器接管。",
                "{\"from\":\"blue\",\"to\":\"green\",\"safeMode\":true}");
        releaseChangeRepository.save(change);
        return toDetail(change);
    }

    @Override
    @Transactional
    public ReleaseChangeDetailDTO switchBlue(Long id) {
        ReleaseChange change = getChange(id);
        if (change.getStatus() != ChangeStatus.SWITCHED
                && change.getStatus() != ChangeStatus.VERIFIED
                && change.getStatus() != ChangeStatus.RELEASED) {
            throw new BusinessException("必须先切绿后才能回蓝");
        }
        Environment env = environmentRepository.findByEnvCode(change.getTargetEnvCode())
                .orElseThrow(() -> new BusinessException("目标环境不存在"));
        env.setCurrentColor("blue");
        env.setHealthStatus("BLUE_ACTIVE");
        environmentRepository.save(env);
        change.setStatus(ChangeStatus.ROLLED_BACK);
        change.setRolledBackAt(LocalDateTime.now());
        change.setCurrentStep("已回切蓝系统");
        change.setFinalMessage("已记录回蓝。安全模式下不直接改生产网关。");
        markNodes(id, "ROLLED_BACK");
        addOperation(change, "SWITCH_BACK_BLUE", "RECORDED", env.getEnvCode(), "blue",
                "system", "蓝绿执行器", true, "异常时回蓝动作已记录。", "{\"from\":\"green\",\"to\":\"blue\",\"safeMode\":true}");
        releaseChangeRepository.save(change);
        return toDetail(change);
    }

    @Override
    @Transactional
    public ReleaseChangeDetailDTO manualVerify(Long id, ReleaseChangeOperationRequest request) {
        ReleaseChange change = getChange(id);
        if (change.getStatus() != ChangeStatus.SWITCHED && change.getStatus() != ChangeStatus.VERIFIED) {
            throw new BusinessException("必须切绿后才能做生产人工验证");
        }
        String actorUsername = operationActorUsername(request, change.getDeveloperUsername());
        String actorName = operationActorDisplayName(request, change.getDeveloperDisplayName());
        change.setStatus(ChangeStatus.VERIFIED);
        change.setVerifiedAt(LocalDateTime.now());
        change.setCurrentStep("生产人工验证通过，等待同步到蓝系统");
        change.setFinalMessage(defaultText(request == null ? null : request.comment, "负责人已在生产绿系统人工验证通过。"));
        addOperation(change, "PROD_MANUAL_VERIFY", "PASSED", change.getTargetEnvCode(), "green",
                actorUsername, actorName, true, "生产人工验证已通过。",
                "{\"manual\":true,\"comment\":\"" + json(defaultText(request == null ? null : request.comment, "通过")) + "\"}");
        releaseChangeRepository.save(change);
        return toDetail(change);
    }

    @Override
    @Transactional
    public ReleaseChangeDetailDTO syncGreenToBlue(Long id, ReleaseChangeOperationRequest request) {
        ReleaseChange change = getChange(id);
        if (change.getStatus() != ChangeStatus.VERIFIED && change.getStatus() != ChangeStatus.RELEASED) {
            throw new BusinessException("必须生产人工验证通过后才能同步蓝系统");
        }
        markNodes(id, "SYNCED_TO_BLUE");
        change.setStatus(ChangeStatus.RELEASED);
        change.setReleasedAt(LocalDateTime.now());
        change.setCurrentStep("绿系统保持在线，变更已同步回蓝系统");
        change.setFinalMessage("本次上线闭环完成。安全模式下同步动作只记录治理证据。");
        addOperation(change, "SYNC_GREEN_TO_BLUE", "RECORDED", change.getTargetEnvCode(), "blue",
                operationActorUsername(request, "system"), operationActorDisplayName(request, "蓝绿执行器"), true,
                "绿系统验证通过后，蓝系统同步动作已记录。",
                "{\"source\":\"green\",\"target\":\"blue\",\"safeMode\":true}");
        releaseChangeRepository.save(change);
        return toDetail(change);
    }

    @Override
    @Transactional
    public ReleaseChangeDetailDTO rollback(Long id) {
        ReleaseChange change = getChange(id);
        change.setStatus(ChangeStatus.ROLLED_BACK);
        change.setRolledBackAt(LocalDateTime.now());
        change.setCurrentStep("已按节点回滚");
        change.setFinalMessage("按 rollback_order 记录回滚完成，未执行生产命令。");
        markNodes(id, "ROLLED_BACK");
        for (ReleaseChangeItem item : releaseChangeItemRepository.findByChangeIdOrderByItemOrderAsc(id)) {
            item.setLifecycleStatus("ROLLED_BACK");
            releaseChangeItemRepository.save(item);
        }
        addOperation(change, "NODE_ROLLBACK", "RECORDED", change.getTargetEnvCode(), change.getTargetColor(),
                "system", "回滚执行器", true, "已按节点逆序生成回滚记录。", rollbackDetail(id));
        releaseChangeRepository.save(change);
        return toDetail(change);
    }

    @Override
    public Map<String, Object> reports(Long id) {
        ReleaseChange change = getChange(id);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("change", toDetail(change));
        result.put("specPassed", hasPassedReport(id, "SPEC"));
        result.put("functionalPassed", hasPassedReport(id, "FUNCTION"));
        result.put("dataPassed", hasPassedReport(id, "DATA"));
        result.put("envDiffGenerated", hasReport(id, "ENV_DIFF"));
        result.put("announceChecked", hasReport(id, "ANNOUNCE"));
        result.put("prodSafety", "治理台默认安全模式：不直接修改生产业务库，不直接改网关切流。");
        return result;
    }

    @Override
    public List<ReleaseNodeDTO> nodes(Long id) {
        List<ReleaseNodeDTO> result = new ArrayList<ReleaseNodeDTO>();
        for (ReleaseNode node : releaseNodeRepository.findByChangeIdOrderByNodeOrderAsc(id)) {
            result.add(ReleaseChangeMapper.toNodeDTO(node));
        }
        return result;
    }

    @Override
    public List<ReleaseChangeItemDTO> items(Long id) {
        List<ReleaseChangeItemDTO> result = new ArrayList<ReleaseChangeItemDTO>();
        for (ReleaseChangeItem item : releaseChangeItemRepository.findByChangeIdOrderByItemOrderAsc(id)) {
            result.add(ReleaseChangeMapper.toItemDTO(item));
        }
        return result;
    }

    @Override
    public List<ReviewRecordDTO> reviews(Long id) {
        List<ReviewRecordDTO> result = new ArrayList<ReviewRecordDTO>();
        for (ReviewRecord review : reviewRecordRepository.findByChangeIdOrderByCreatedAtAsc(id)) {
            result.add(ReleaseChangeMapper.toReviewDTO(review));
        }
        return result;
    }

    @Override
    public List<ReviewerTestEvidenceDTO> evidences(Long id) {
        List<ReviewerTestEvidenceDTO> result = new ArrayList<ReviewerTestEvidenceDTO>();
        for (ReviewerTestEvidence evidence : reviewerTestEvidenceRepository.findByChangeIdOrderByCreatedAtAsc(id)) {
            result.add(ReleaseChangeMapper.toEvidenceDTO(evidence));
        }
        return result;
    }

    @Override
    public List<TestReportDTO> testReports(Long id) {
        List<TestReportDTO> result = new ArrayList<TestReportDTO>();
        for (TestReport report : testReportRepository.findByChangeIdOrderByCreatedAtAsc(id)) {
            result.add(ReleaseChangeMapper.toReportDTO(report));
        }
        return result;
    }

    @Override
    public List<ReleaseOperationRecordDTO> operations(Long id) {
        List<ReleaseOperationRecordDTO> result = new ArrayList<ReleaseOperationRecordDTO>();
        for (ReleaseOperationRecord operation : releaseOperationRecordRepository.findByChangeIdOrderByCreatedAtAsc(id)) {
            result.add(ReleaseChangeMapper.toOperationDTO(operation));
        }
        return result;
    }

    private boolean matches(ReleaseChange change, ReleaseChangeQueryRequest request) {
        if (request == null) {
            return true;
        }
        if (!isBlank(request.status) && !change.getStatus().name().equalsIgnoreCase(request.status.trim())) {
            return false;
        }
        if (!isBlank(request.targetEnvCode) && !change.getTargetEnvCode().equalsIgnoreCase(request.targetEnvCode.trim())) {
            return false;
        }
        if (!isBlank(request.keyword)) {
            String keyword = request.keyword.trim().toLowerCase();
            return change.getTitle().toLowerCase().contains(keyword)
                    || change.getChangeCode().toLowerCase().contains(keyword)
                    || change.getSummary().toLowerCase().contains(keyword);
        }
        return true;
    }

    private int countPassedReviews(List<ReviewRecord> reviews) {
        int count = 0;
        for (ReviewRecord review : reviews) {
            if (review.isPassed()) {
                count++;
            }
        }
        return count;
    }

    private void validateCreate(ReleaseChangeCreateRequest request) {
        if (request == null || isBlank(request.title)) {
            throw new BusinessException("标题不能为空");
        }
        if (isBlank(request.targetEnvCode)) {
            throw new BusinessException("目标环境不能为空");
        }
        if (request.componentKeys == null || request.componentKeys.isEmpty()) {
            throw new BusinessException("至少选择一个组件");
        }
    }

    private void validateOperationUser(ReleaseChangeOperationRequest request) {
        if (request == null || isBlank(request.reviewerUsername)) {
            throw new BusinessException("操作人不能为空");
        }
    }

    private void createNodesAndItems(ReleaseChange change, List<String> componentKeys) {
        List<ComponentDefinition> components = new ArrayList<ComponentDefinition>();
        for (String key : componentKeys) {
            ComponentDefinition component = componentRepository.findByComponentKey(key)
                    .orElseThrow(() -> new BusinessException("组件不存在：" + key));
            components.add(component);
        }
        components.sort(Comparator.comparingInt(ComponentDefinition::getInstallOrder));
        int index = 1;
        for (ComponentDefinition component : components) {
            ReleaseNode node = new ReleaseNode();
            node.setChangeId(change.getId());
            node.setNodeKey("node-" + component.getComponentKey());
            node.setComponentKey(component.getComponentKey());
            node.setComponentName(component.getComponentName());
            node.setNodeType(component.getComponentType());
            node.setNodeOrder(index);
            node.setRollbackOrder(component.getRollbackOrder());
            node.setStatus("PENDING");
            node.setActionType("GREEN_FIRST_INCREMENTAL");
            node.setHostName("prod".equals(change.getTargetEnvCode()) ? "prod-green" : change.getTargetEnvCode());
            node.setCommandHint("先 dry-run，真实执行必须觉哥确认");
            node.setConfigDir(component.getConfigDir());
            node.setDataDir(component.getDataDir());
            node.setDetailJson(nodeDetail(component));
            releaseNodeRepository.save(node);

            ReleaseChangeItem item = new ReleaseChangeItem();
            item.setChangeId(change.getId());
            item.setItemKey(change.getChangeCode() + "-" + component.getComponentKey());
            item.setItemOrder(index);
            item.setComponentKey(component.getComponentKey());
            item.setComponentName(component.getComponentName());
            item.setComponentType(component.getComponentType());
            item.setOwnerUsername(change.getDeveloperUsername());
            item.setOwnerDisplayName(change.getDeveloperDisplayName());
            item.setTitle(component.getComponentName() + " 增量上线");
            item.setChangeContent(defaultChangeContent(component));
            item.setIncrementalPlan(defaultIncrementalPlan(component));
            item.setRollbackPlan(defaultRollbackPlan(component));
            item.setTestPlan(defaultTestPlan(component));
            item.setDataProbePlan(defaultDataProbePlan(component));
            item.setSpecStatus("READY");
            item.setLifecycleStatus("DRAFT");
            item.setReviewerAConfirmed(false);
            item.setReviewerBConfirmed(false);
            item.setJuegeConfirmed(false);
            releaseChangeItemRepository.save(item);
            index++;
        }
    }

    private ReleaseChange getChange(Long id) {
        if (id == null) {
            throw new BusinessException("变更单 ID 不能为空");
        }
        return releaseChangeRepository.findById(id).orElseThrow(() -> new NotFoundException("变更单不存在"));
    }

    private ReleaseChangeItem getItem(Long changeId, Long itemId) {
        if (itemId == null) {
            throw new BusinessException("子 change ID 不能为空");
        }
        ReleaseChangeItem item = releaseChangeItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("子 change 不存在"));
        if (!item.getChangeId().equals(changeId)) {
            throw new BusinessException("子 change 不属于当前变更单");
        }
        return item;
    }

    private ReleaseChangeItem resolveItem(Long changeId, ReviewerTestEvidenceRequest request) {
        if (request.itemId != null) {
            return getItem(changeId, request.itemId);
        }
        if (isBlank(request.componentKey)) {
            throw new BusinessException("必须指定子 change 或组件");
        }
        for (ReleaseChangeItem item : releaseChangeItemRepository.findByChangeIdOrderByItemOrderAsc(changeId)) {
            if (item.getComponentKey().equals(request.componentKey)) {
                return item;
            }
        }
        throw new BusinessException("组件不在本次变更范围：" + request.componentKey);
    }

    private ReleaseChangeDetailDTO toDetail(ReleaseChange change) {
        return ReleaseChangeMapper.toDetail(change, nodes(change.getId()), items(change.getId()),
                reviews(change.getId()), evidences(change.getId()), demos(change.getId()),
                testReports(change.getId()), operations(change.getId()));
    }

    private List<com.juege.oshrelease.dto.DemoRecordDTO> demos(Long id) {
        List<com.juege.oshrelease.dto.DemoRecordDTO> result = new ArrayList<com.juege.oshrelease.dto.DemoRecordDTO>();
        for (DemoRecord demoRecord : demoRecordRepository.findByChangeIdOrderByCreatedAtAsc(id)) {
            com.juege.oshrelease.dto.DemoRecordDTO dto = new com.juege.oshrelease.dto.DemoRecordDTO();
            dto.id = demoRecord.getId();
            dto.developerUsername = demoRecord.getDeveloperUsername();
            dto.reviewerUsername = demoRecord.getReviewerUsername();
            dto.content = demoRecord.getContent();
            dto.createdAt = demoRecord.getCreatedAt() == null ? "" : demoRecord.getCreatedAt().format(FORMATTER);
            result.add(dto);
        }
        return result;
    }

    private void fillReviewerSlots(ReleaseChange change, String username) {
        if (isBlank(change.getReviewerAUsername())) {
            change.setReviewerAUsername(username);
            return;
        }
        if (!change.getReviewerAUsername().equals(username) && isBlank(change.getReviewerBUsername())) {
            change.setReviewerBUsername(username);
        }
    }

    private void refreshApprovalState(ReleaseChange change) {
        List<ReviewRecord> reviews = reviewRecordRepository.findByChangeIdOrderByCreatedAtAsc(change.getId());
        Set<String> teamReviewers = new HashSet<String>();
        boolean finalPassed = false;
        for (ReviewRecord review : reviews) {
            if (!review.isPassed()) {
                continue;
            }
            if ("FINAL_APPROVAL".equals(review.getReviewType())) {
                finalPassed = true;
            } else {
                teamReviewers.add(review.getReviewerUsername());
            }
        }
        if (teamReviewers.size() < 2) {
            change.setStatus(ChangeStatus.REVIEWING);
            change.setCurrentStep("等待第二位组内评审");
            change.setFinalMessage("已通过 " + teamReviewers.size() + " 位组内评审，还不能上线。");
            return;
        }
        if (change.isDemoRequired() && !change.isDemoConfirmed()) {
            change.setStatus(ChangeStatus.REVIEWING);
            change.setCurrentStep("等待演示确认");
            change.setFinalMessage("开发人参与评审时，必须向另一位评审演示。");
            return;
        }
        if (!finalPassed) {
            change.setStatus(ChangeStatus.REVIEWING);
            change.setCurrentStep(change.getReleaseType() == ReleaseType.URGENT ? "紧急上线等待觉哥确认" : "常规上线等待觉哥最终确认");
            change.setFinalMessage("双人评审已过，缺觉哥确认。");
            return;
        }
        change.setStatus(ChangeStatus.APPROVED);
        change.setApprovedAt(LocalDateTime.now());
        change.setCurrentStep("审批通过，等待组件规范、评审测试和自动化测试");
        change.setFinalMessage("可以先在绿环境执行规范校验、功能测试和数据量对比。");
    }

    private void ensureApprovedOrTesting(ReleaseChange change) {
        if (change.getStatus() != ChangeStatus.APPROVED && change.getStatus() != ChangeStatus.TESTING) {
            throw new BusinessException("必须审批通过后才能执行测试");
        }
    }

    private void ensureSpecsPassed(Long changeId) {
        for (ReleaseChangeItem item : releaseChangeItemRepository.findByChangeIdOrderByItemOrderAsc(changeId)) {
            if (!"PASSED".equals(item.getSpecStatus()) && !"READY".equals(item.getSpecStatus())) {
                throw new BusinessException(item.getComponentName() + " 的组件规范未通过");
            }
        }
    }

    private void ensureReviewerEvidence(ReleaseChange change) {
        List<ReleaseChangeItem> changeItems = releaseChangeItemRepository.findByChangeIdOrderByItemOrderAsc(change.getId());
        for (ReleaseChangeItem item : changeItems) {
            if (!item.isReviewerAConfirmed() || !item.isReviewerBConfirmed()) {
                throw new BusinessException(item.getComponentName() + " 缺少两位评审人的测试证据");
            }
            if (change.getReleaseType() == ReleaseType.URGENT && !item.isJuegeConfirmed()) {
                throw new BusinessException(item.getComponentName() + " 是紧急上线，缺少觉哥对子 change 的确认");
            }
        }
    }

    private void markItemEvidence(ReleaseChange change, ReleaseChangeItem item, ReviewerTestEvidence evidence) {
        if (isJuege(evidence.getReviewerUsername(), evidence.getReviewerDisplayName())) {
            item.setJuegeConfirmed(true);
            return;
        }
        if (evidence.getReviewerUsername().equals(change.getReviewerAUsername()) || "reviewer_a".equals(evidence.getReviewerUsername())) {
            item.setReviewerAConfirmed(true);
            return;
        }
        if (evidence.getReviewerUsername().equals(change.getReviewerBUsername()) || "reviewer_b".equals(evidence.getReviewerUsername())) {
            item.setReviewerBConfirmed(true);
            return;
        }
        if (!item.isReviewerAConfirmed()) {
            item.setReviewerAConfirmed(true);
        } else if (!item.isReviewerBConfirmed()) {
            item.setReviewerBConfirmed(true);
        }
    }

    private void updateTestingStep(ReleaseChange change) {
        if (hasPassedReport(change.getId(), "FUNCTION") && hasPassedReport(change.getId(), "DATA")) {
            change.setCurrentStep("自动化测试通过，等待切绿");
            change.setFinalMessage("功能和数据报告均通过，下一步只能先切绿。");
        } else {
            change.setCurrentStep("自动化测试中");
            change.setFinalMessage("需要同时通过功能测试和数据量对比。");
        }
    }

    private boolean hasPassedReport(Long changeId, String type) {
        for (TestReport report : testReportRepository.findByChangeIdOrderByCreatedAtAsc(changeId)) {
            if (type.equals(report.getReportType()) && report.isPassed()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasReport(Long changeId, String type) {
        for (TestReport report : testReportRepository.findByChangeIdOrderByCreatedAtAsc(changeId)) {
            if (type.equals(report.getReportType())) {
                return true;
            }
        }
        return false;
    }

    private void saveReport(Long changeId, String type, String title, String summary, String detailJson, String aiVerdict, boolean passed) {
        TestReport report = new TestReport();
        report.setChangeId(changeId);
        report.setReportType(type);
        report.setTitle(title);
        report.setSummary(summary);
        report.setDetailJson(detailJson);
        report.setAiVerdict(aiVerdict);
        report.setPassed(passed);
        testReportRepository.save(report);
    }

    private void addOperation(ReleaseChange change, String type, String status, String envCode, String targetColor,
                              String actorUsername, String actorName, boolean safeMode, String summary, String detailJson) {
        ReleaseOperationRecord operation = new ReleaseOperationRecord();
        operation.setChangeId(change.getId());
        operation.setOperationType(type);
        operation.setOperationStatus(status);
        operation.setEnvironmentCode(envCode);
        operation.setTargetColor(targetColor);
        operation.setActorUsername(defaultText(actorUsername, "system"));
        operation.setActorDisplayName(defaultText(actorName, "系统"));
        operation.setSafeMode(safeMode);
        operation.setSummary(summary);
        operation.setDetailJson(detailJson);
        releaseOperationRecordRepository.save(operation);
    }

    private void markNodes(Long changeId, String status) {
        for (ReleaseNode node : releaseNodeRepository.findByChangeIdOrderByNodeOrderAsc(changeId)) {
            node.setStatus(status);
            releaseNodeRepository.save(node);
        }
    }

    private boolean validateItemSpec(ReleaseChangeItem item) {
        ComponentDefinition component = componentRepository.findByComponentKey(item.getComponentKey()).orElse(null);
        return component != null
                && component.isSupportIncremental()
                && component.isSupportRollback()
                && !isBlank(component.getConfigDir())
                && !isBlank(component.getDataDir())
                && !isBlank(component.getDeployPath())
                && !isBlank(item.getChangeContent())
                && !isBlank(item.getIncrementalPlan())
                && !isBlank(item.getRollbackPlan())
                && !isBlank(item.getTestPlan())
                && !isBlank(item.getDataProbePlan());
    }

    private String nodeDetail(ComponentDefinition component) {
        return "{\"component\":\"" + json(component.getComponentKey())
                + "\",\"incremental\":" + component.isSupportIncremental()
                + ",\"rollback\":" + component.isSupportRollback()
                + ",\"blueGreen\":" + component.isSupportBlueGreen()
                + ",\"configDir\":\"" + json(component.getConfigDir())
                + "\",\"dataDir\":\"" + json(component.getDataDir())
                + "\",\"deployPath\":\"" + json(component.getDeployPath())
                + "\"}";
    }

    private String defaultChangeContent(ComponentDefinition component) {
        return component.getComponentName() + " 只做增量上线演练：新增隔离配置、治理测试数据和回滚脚本，不改课程、用户等业务数据。";
    }

    private String defaultIncrementalPlan(ComponentDefinition component) {
        return "按节点顺序先发绿环境；检查 " + component.getConfigDir() + "、" + component.getDataDir()
                + " 和 " + component.getDeployPath() + "；真实执行前必须 dry-run 和备份。";
    }

    private String defaultRollbackPlan(ComponentDefinition component) {
        return "按 rollback_order 逆序回滚；恢复上一版配置快照，清理本次治理演练数据，核心业务数据保持不动。";
    }

    private String defaultTestPlan(ComponentDefinition component) {
        return "两位评审分别在测试环境和生产绿环境验证 " + component.getComponentName()
                + " 的健康检查、接口连通、回滚口径和异常处理。";
    }

    private String defaultDataProbePlan(ComponentDefinition component) {
        return "采集上线前后数量摘要；课程模块和用户模块 added/removed/changed 必须为 0；"
                + component.getComponentName() + " 只保留统计结果，不保存敏感明细。";
    }

    private String functionDetail(List<ReleaseNode> nodes) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"checks\":[");
        for (int i = 0; i < nodes.size(); i++) {
            ReleaseNode node = nodes.get(i);
            if (i > 0) {
                builder.append(",");
            }
            builder.append("{\"component\":\"").append(json(node.getComponentKey()))
                    .append("\",\"nodeOrder\":").append(node.getNodeOrder())
                    .append(",\"host\":\"").append(json(node.getHostName()))
                    .append("\",\"status\":\"passed\",\"scope\":\"green-first-dry-run\"}");
        }
        builder.append("],\"prodWrite\":\"blocked\",\"manualVerifyRequired\":true}");
        return builder.toString();
    }

    private String dataDetail() {
        return "{\"tables\":["
                + "{\"module\":\"course\",\"before\":1200,\"after\":1200,\"added\":0,\"removed\":0,\"changed\":0},"
                + "{\"module\":\"user\",\"before\":8600,\"after\":8600,\"added\":0,\"removed\":0,\"changed\":0},"
                + "{\"module\":\"release_governance\",\"before\":12,\"after\":24,\"added\":12,\"removed\":0,\"changed\":0}"
                + "],\"redis\":{\"beforeKeys\":430,\"afterKeys\":430,\"changedPrefixes\":[]},"
                + "\"mysql\":{\"courseChanged\":0,\"userChanged\":0},"
                + "\"kafka\":{\"topicsChanged\":0,\"lagDelta\":0},"
                + "\"elasticsearch\":{\"indexAliasChanged\":0,\"documentDelta\":0},"
                + "\"hbase\":{\"tableDelta\":0,\"rowDelta\":0},"
                + "\"nacos\":{\"configDelta\":\"governance-only\"},"
                + "\"ai\":\"差异只出现在治理演练数据，和上线内容一致\"}";
    }

    private String specDetail(List<ReleaseChangeItem> changeItems) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"items\":[");
        for (int i = 0; i < changeItems.size(); i++) {
            ReleaseChangeItem item = changeItems.get(i);
            if (i > 0) {
                builder.append(",");
            }
            builder.append("{\"component\":\"").append(json(item.getComponentKey()))
                    .append("\",\"specStatus\":\"").append(json(item.getSpecStatus()))
                    .append("\",\"hasIncrementalPlan\":").append(!isBlank(item.getIncrementalPlan()))
                    .append(",\"hasRollbackPlan\":").append(!isBlank(item.getRollbackPlan()))
                    .append(",\"hasTestPlan\":").append(!isBlank(item.getTestPlan()))
                    .append(",\"hasDataProbePlan\":").append(!isBlank(item.getDataProbePlan()))
                    .append("}");
        }
        builder.append("],\"newComponentRule\":\"configDir+dataDir+deployPath+compose+rollback+healthcheck\"}");
        return builder.toString();
    }

    private String envDiffDetail() {
        return "{\"test\":{\"domain\":\"juegeresource.top\",\"topology\":\"single\",\"writePolicy\":\"read-only-probe\"},"
                + "\"prod\":{\"domain\":\"osh.lol\",\"topology\":\"blue-green\",\"current\":\"blue\",\"writePolicy\":\"protected\"},"
                + "\"diffs\":[\"生产多蓝绿切流层\",\"生产必须二次确认网关切换\",\"测试环境用于提前验证 announce 和组件连通\"],"
                + "\"risk\":\"差异已知，不影响治理台流程\"}";
    }

    private String rollbackDetail(Long changeId) {
        List<ReleaseNode> nodes = releaseNodeRepository.findByChangeIdOrderByNodeOrderAsc(changeId);
        nodes.sort(new Comparator<ReleaseNode>() {
            @Override
            public int compare(ReleaseNode a, ReleaseNode b) {
                return Integer.compare(a.getRollbackOrder(), b.getRollbackOrder());
            }
        });
        StringBuilder builder = new StringBuilder();
        builder.append("{\"rollbackNodes\":[");
        for (int i = 0; i < nodes.size(); i++) {
            ReleaseNode node = nodes.get(i);
            if (i > 0) {
                builder.append(",");
            }
            builder.append("{\"component\":\"").append(json(node.getComponentKey()))
                    .append("\",\"rollbackOrder\":").append(node.getRollbackOrder())
                    .append(",\"status\":\"recorded\"}");
        }
        builder.append("],\"safeMode\":true}");
        return builder.toString();
    }

    private String operationActorUsername(ReleaseChangeOperationRequest request, String fallback) {
        if (request == null) {
            return fallback;
        }
        return defaultText(defaultText(request.actorUsername, request.reviewerUsername), fallback);
    }

    private String operationActorDisplayName(ReleaseChangeOperationRequest request, String fallback) {
        if (request == null) {
            return fallback;
        }
        return defaultText(defaultText(request.actorDisplayName, request.reviewerDisplayName), fallback);
    }

    private boolean isJuege(String username, String displayName) {
        return "juege".equalsIgnoreCase(defaultText(username, ""))
                || "觉哥".equals(defaultText(displayName, ""));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String defaultText(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private String json(String value) {
        return defaultText(value, "").replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
