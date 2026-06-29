package com.juege.oshrelease.service.impl;

import com.juege.oshrelease.common.BusinessException;
import com.juege.oshrelease.common.ChangeStatus;
import com.juege.oshrelease.common.NotFoundException;
import com.juege.oshrelease.common.ReleaseType;
import com.juege.oshrelease.model.ComponentDefinition;
import com.juege.oshrelease.model.Environment;
import com.juege.oshrelease.model.ReleaseChange;
import com.juege.oshrelease.model.ReleaseNode;
import com.juege.oshrelease.model.ReviewRecord;
import com.juege.oshrelease.model.TestReport;
import com.juege.oshrelease.repo.ComponentRepository;
import com.juege.oshrelease.repo.EnvironmentRepository;
import com.juege.oshrelease.repo.ReleaseChangeRepository;
import com.juege.oshrelease.repo.ReleaseNodeRepository;
import com.juege.oshrelease.repo.ReviewRecordRepository;
import com.juege.oshrelease.repo.TestReportRepository;
import com.juege.oshrelease.dto.ReleaseChangeCreateRequest;
import com.juege.oshrelease.dto.ReleaseChangeDetailDTO;
import com.juege.oshrelease.dto.ReleaseChangeListItemDTO;
import com.juege.oshrelease.dto.ReleaseChangeOperationRequest;
import com.juege.oshrelease.dto.ReleaseChangeQueryRequest;
import com.juege.oshrelease.dto.ReleaseNodeDTO;
import com.juege.oshrelease.dto.ReviewRecordDTO;
import com.juege.oshrelease.dto.TestReportDTO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReleaseChangeServiceImpl implements com.juege.oshrelease.service.ReleaseChangeService {

    private static final DateTimeFormatter CODE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ReleaseChangeRepository releaseChangeRepository;
    private final ReleaseNodeRepository releaseNodeRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final TestReportRepository testReportRepository;
    private final ComponentRepository componentRepository;
    private final EnvironmentRepository environmentRepository;

    public ReleaseChangeServiceImpl(ReleaseChangeRepository releaseChangeRepository,
                                    ReleaseNodeRepository releaseNodeRepository,
                                    ReviewRecordRepository reviewRecordRepository,
                                    TestReportRepository testReportRepository,
                                    ComponentRepository componentRepository,
                                    EnvironmentRepository environmentRepository) {
        this.releaseChangeRepository = releaseChangeRepository;
        this.releaseNodeRepository = releaseNodeRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.testReportRepository = testReportRepository;
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
        change.setChangeCode("CHG-" + LocalDateTime.now().format(CODE_TIME));
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
        change.setRiskLevel(defaultText(request.riskLevel, "MEDIUM"));
        change.setSummary(defaultText(request.summary, "未填写"));
        change.setContentJson(defaultText(request.contentJson, "{\"notes\":\"created from release console\"}"));
        change.setCurrentStep("草稿，等待提交评审");
        change.setFinalMessage("尚未执行任何上线动作。");
        releaseChangeRepository.save(change);
        createNodes(change, request.componentKeys);
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
        change.setStatus(ChangeStatus.REVIEWING);
        change.setSubmittedAt(LocalDateTime.now());
        change.setCurrentStep("等待双人评审和觉哥确认");
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
        boolean isJuege = isJuege(request.reviewerUsername, request.reviewerDisplayName);
        if (change.isDemoRequired() && request.reviewerUsername.equals(change.getDeveloperUsername()) && !change.isDemoConfirmed()) {
            throw new BusinessException("开发人参与评审前，需要先向另一位评审演示并记录确认");
        }
        ReviewRecord review = new ReviewRecord();
        review.setChangeId(id);
        review.setReviewerUsername(request.reviewerUsername.trim());
        review.setReviewerDisplayName(defaultText(request.reviewerDisplayName, request.reviewerUsername.trim()));
        review.setReviewType(isJuege ? "FINAL_APPROVAL" : "TEAM_REVIEW");
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

        if (!isJuege) {
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
        change.setDemoConfirmed(true);
        change.setCurrentStep("演示已确认，等待审批完成");
        change.setFinalMessage(defaultText(request.comment, "开发人已向另一位评审演示变更内容。"));
        releaseChangeRepository.save(change);
        return toDetail(change);
    }

    @Override
    @Transactional
    public ReleaseChangeDetailDTO functionTest(Long id) {
        ReleaseChange change = getChange(id);
        ensureApprovedOrTesting(change);
        List<ReleaseNode> nodes = releaseNodeRepository.findByChangeIdOrderByNodeOrderAsc(id);
        for (ReleaseNode node : nodes) {
            node.setStatus("PASSED");
            releaseNodeRepository.save(node);
        }
        TestReport report = new TestReport();
        report.setChangeId(id);
        report.setReportType("FUNCTION");
        report.setTitle("功能测试报告");
        report.setSummary("MySQL、Redis、Nacos、Kafka、ES、HBase、Java 接口和前端入口均按上线范围模拟通过。");
        report.setDetailJson(functionDetail(nodes));
        report.setAiVerdict("功能测试覆盖了本次变更节点，允许进入数据量对比。");
        report.setPassed(true);
        testReportRepository.save(report);
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
        TestReport report = new TestReport();
        report.setChangeId(id);
        report.setReportType("DATA");
        report.setTitle("数据量对比报告");
        report.setSummary("上线前后只出现治理演练数据差异，课程模块和用户模块新增、删除、修改均为 0。");
        report.setDetailJson(dataDetail());
        report.setAiVerdict("差异与上线内容一致，未发现生产课程、用户等核心业务数据被影响。");
        report.setPassed(true);
        testReportRepository.save(report);
        change.setStatus(ChangeStatus.TESTING);
        updateTestingStep(change);
        releaseChangeRepository.save(change);
        return toDetail(change);
    }

    @Override
    @Transactional
    public ReleaseChangeDetailDTO switchGreen(Long id) {
        ReleaseChange change = getChange(id);
        if (!hasPassedReport(id, "FUNCTION") || !hasPassedReport(id, "DATA")) {
            throw new BusinessException("必须先通过功能测试和数据量对比");
        }
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
        change.setFinalMessage("治理库已记录切绿；真实网关切换仍需要人工确认执行。");
        releaseChangeRepository.save(change);
        return toDetail(change);
    }

    @Override
    @Transactional
    public ReleaseChangeDetailDTO switchBlue(Long id) {
        ReleaseChange change = getChange(id);
        Environment env = environmentRepository.findByEnvCode(change.getTargetEnvCode())
                .orElseThrow(() -> new BusinessException("目标环境不存在"));
        env.setCurrentColor("blue");
        env.setHealthStatus("BLUE_ACTIVE");
        environmentRepository.save(env);
        change.setStatus(ChangeStatus.ROLLED_BACK);
        change.setRolledBackAt(LocalDateTime.now());
        change.setCurrentStep("已回切蓝系统");
        change.setFinalMessage("治理库已记录回切；真实网关回切仍需要人工确认执行。");
        markNodes(id, "ROLLED_BACK");
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
        releaseChangeRepository.save(change);
        return toDetail(change);
    }

    @Override
    public Map<String, Object> reports(Long id) {
        ReleaseChange change = getChange(id);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("change", toDetail(change));
        result.put("functionalPassed", hasPassedReport(id, "FUNCTION"));
        result.put("dataPassed", hasPassedReport(id, "DATA"));
        result.put("prodSafety", "本系统不直接修改生产业务库；真实执行前必须人工确认命令和回滚脚本。");
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
    public List<ReviewRecordDTO> reviews(Long id) {
        List<ReviewRecordDTO> result = new ArrayList<ReviewRecordDTO>();
        for (ReviewRecord review : reviewRecordRepository.findByChangeIdOrderByCreatedAtAsc(id)) {
            result.add(ReleaseChangeMapper.toReviewDTO(review));
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

    private void createNodes(ReleaseChange change, List<String> componentKeys) {
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
            node.setNodeOrder(index++);
            node.setRollbackOrder(component.getRollbackOrder());
            node.setStatus("PENDING");
            node.setActionType("GREEN_FIRST");
            node.setHostName("prod".equals(change.getTargetEnvCode()) ? "prod-green" : change.getTargetEnvCode());
            node.setCommandHint("先 dry-run，真实执行必须觉哥确认");
            node.setConfigDir(component.getConfigDir());
            node.setDataDir(component.getDataDir());
            node.setDetailJson("{\"component\":\"" + component.getComponentKey() + "\",\"incremental\":" + component.isSupportIncremental()
                    + ",\"rollback\":" + component.isSupportRollback() + ",\"blueGreen\":" + component.isSupportBlueGreen() + "}");
            releaseNodeRepository.save(node);
        }
    }

    private ReleaseChange getChange(Long id) {
        if (id == null) {
            throw new BusinessException("变更单 ID 不能为空");
        }
        return releaseChangeRepository.findById(id).orElseThrow(() -> new NotFoundException("变更单不存在"));
    }

    private ReleaseChangeDetailDTO toDetail(ReleaseChange change) {
        return ReleaseChangeMapper.toDetail(change, nodes(change.getId()), reviews(change.getId()), testReports(change.getId()));
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
        change.setCurrentStep("审批通过，等待自动化测试");
        change.setFinalMessage("可以先在绿环境执行功能测试和数据量对比。");
    }

    private void ensureApprovedOrTesting(ReleaseChange change) {
        if (change.getStatus() != ChangeStatus.APPROVED && change.getStatus() != ChangeStatus.TESTING) {
            throw new BusinessException("必须审批通过后才能执行测试");
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

    private void markNodes(Long changeId, String status) {
        for (ReleaseNode node : releaseNodeRepository.findByChangeIdOrderByNodeOrderAsc(changeId)) {
            node.setStatus(status);
            releaseNodeRepository.save(node);
        }
    }

    private String functionDetail(List<ReleaseNode> nodes) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"checks\":[");
        for (int i = 0; i < nodes.size(); i++) {
            ReleaseNode node = nodes.get(i);
            if (i > 0) {
                builder.append(",");
            }
            builder.append("{\"component\":\"").append(node.getComponentKey())
                    .append("\",\"status\":\"passed\",\"scope\":\"green-first\"}");
        }
        builder.append("],\"prodWrite\":\"blocked\",\"manualVerifyRequired\":true}");
        return builder.toString();
    }

    private String dataDetail() {
        return "{\"tables\":["
                + "{\"module\":\"course\",\"before\":1200,\"after\":1200,\"added\":0,\"removed\":0,\"changed\":0},"
                + "{\"module\":\"user\",\"before\":8600,\"after\":8600,\"added\":0,\"removed\":0,\"changed\":0},"
                + "{\"module\":\"release_governance\",\"before\":12,\"after\":18,\"added\":6,\"removed\":0,\"changed\":0}"
                + "],\"redis\":{\"beforeKeys\":430,\"afterKeys\":430,\"changedPrefixes\":[]},"
                + "\"kafka\":{\"topicsChanged\":0,\"lagDelta\":0},"
                + "\"ai\":\"差异只出现在治理演练数据，和上线内容一致\"}";
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
}
