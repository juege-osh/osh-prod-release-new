package com.juege.oshrelease.service.impl;

import com.juege.oshrelease.dto.ReleaseChangeDetailDTO;
import com.juege.oshrelease.dto.ReleaseChangeItemDTO;
import com.juege.oshrelease.dto.ReleaseChangeListItemDTO;
import com.juege.oshrelease.dto.ReleaseNodeDTO;
import com.juege.oshrelease.dto.ReleaseOperationRecordDTO;
import com.juege.oshrelease.dto.ReviewerTestEvidenceDTO;
import com.juege.oshrelease.dto.ReviewRecordDTO;
import com.juege.oshrelease.dto.DemoRecordDTO;
import com.juege.oshrelease.dto.TestReportDTO;
import com.juege.oshrelease.model.ReleaseChange;
import com.juege.oshrelease.model.ReleaseChangeItem;
import com.juege.oshrelease.model.ReleaseNode;
import com.juege.oshrelease.model.ReleaseOperationRecord;
import com.juege.oshrelease.model.ReviewerTestEvidence;
import com.juege.oshrelease.model.ReviewRecord;
import com.juege.oshrelease.model.TestReport;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class ReleaseChangeMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ReleaseChangeMapper() {
    }

    static ReleaseChangeListItemDTO toListItem(ReleaseChange change, int nodeCount, int approvedReviewCount, int reportCount) {
        ReleaseChangeListItemDTO dto = new ReleaseChangeListItemDTO();
        dto.id = change.getId();
        dto.changeCode = change.getChangeCode();
        dto.title = change.getTitle();
        dto.releaseType = change.getReleaseType().name();
        dto.status = change.getStatus().name();
        dto.targetEnvCode = change.getTargetEnvCode();
        dto.targetColor = change.getTargetColor();
        dto.developerDisplayName = change.getDeveloperDisplayName();
        dto.reviewMode = change.getReviewMode();
        dto.riskLevel = change.getRiskLevel();
        dto.nodeCount = nodeCount;
        dto.approvedReviewCount = approvedReviewCount;
        dto.reportCount = reportCount;
        dto.currentStep = change.getCurrentStep();
        dto.finalMessage = change.getFinalMessage();
        return dto;
    }

    static ReleaseChangeDetailDTO toDetail(ReleaseChange change,
                                           List<ReleaseNodeDTO> nodes,
                                           List<ReleaseChangeItemDTO> items,
                                           List<ReviewRecordDTO> reviews,
                                           List<ReviewerTestEvidenceDTO> evidences,
                                           List<DemoRecordDTO> demos,
                                           List<TestReportDTO> reports,
                                           List<ReleaseOperationRecordDTO> operations) {
        ReleaseChangeDetailDTO dto = new ReleaseChangeDetailDTO();
        dto.id = change.getId();
        dto.changeCode = change.getChangeCode();
        dto.title = change.getTitle();
        dto.projectBranch = change.getProjectBranch();
        dto.releaseType = change.getReleaseType().name();
        dto.targetEnvCode = change.getTargetEnvCode();
        dto.targetColor = change.getTargetColor();
        dto.status = change.getStatus().name();
        dto.developerUsername = change.getDeveloperUsername();
        dto.developerDisplayName = change.getDeveloperDisplayName();
        dto.reviewerAUsername = change.getReviewerAUsername();
        dto.reviewerBUsername = change.getReviewerBUsername();
        dto.reviewMode = change.getReviewMode();
        dto.demoRequired = change.isDemoRequired();
        dto.demoConfirmed = change.isDemoConfirmed();
        dto.riskLevel = change.getRiskLevel();
        dto.summary = change.getSummary();
        dto.contentJson = change.getContentJson();
        dto.currentStep = change.getCurrentStep();
        dto.finalMessage = change.getFinalMessage();
        dto.nodes = nodes;
        dto.items = items;
        dto.reviews = reviews;
        dto.evidences = evidences;
        dto.demos = demos;
        dto.reports = reports;
        dto.operations = operations;
        return dto;
    }

    static ReleaseChangeItemDTO toItemDTO(ReleaseChangeItem item) {
        ReleaseChangeItemDTO dto = new ReleaseChangeItemDTO();
        dto.id = item.getId();
        dto.itemKey = item.getItemKey();
        dto.itemOrder = item.getItemOrder();
        dto.componentKey = item.getComponentKey();
        dto.componentName = item.getComponentName();
        dto.componentType = item.getComponentType();
        dto.ownerUsername = item.getOwnerUsername();
        dto.ownerDisplayName = item.getOwnerDisplayName();
        dto.title = item.getTitle();
        dto.changeContent = item.getChangeContent();
        dto.incrementalPlan = item.getIncrementalPlan();
        dto.rollbackPlan = item.getRollbackPlan();
        dto.testPlan = item.getTestPlan();
        dto.dataProbePlan = item.getDataProbePlan();
        dto.specStatus = item.getSpecStatus();
        dto.lifecycleStatus = item.getLifecycleStatus();
        dto.reviewerAConfirmed = item.isReviewerAConfirmed();
        dto.reviewerBConfirmed = item.isReviewerBConfirmed();
        dto.juegeConfirmed = item.isJuegeConfirmed();
        return dto;
    }

    static ReleaseNodeDTO toNodeDTO(ReleaseNode node) {
        ReleaseNodeDTO dto = new ReleaseNodeDTO();
        dto.id = node.getId();
        dto.nodeKey = node.getNodeKey();
        dto.componentKey = node.getComponentKey();
        dto.componentName = node.getComponentName();
        dto.nodeType = node.getNodeType();
        dto.nodeOrder = node.getNodeOrder();
        dto.rollbackOrder = node.getRollbackOrder();
        dto.status = node.getStatus();
        dto.actionType = node.getActionType();
        dto.hostName = node.getHostName();
        dto.commandHint = node.getCommandHint();
        dto.configDir = node.getConfigDir();
        dto.dataDir = node.getDataDir();
        dto.detailJson = node.getDetailJson();
        return dto;
    }

    static ReviewRecordDTO toReviewDTO(ReviewRecord review) {
        ReviewRecordDTO dto = new ReviewRecordDTO();
        dto.id = review.getId();
        dto.reviewerUsername = review.getReviewerUsername();
        dto.reviewerDisplayName = review.getReviewerDisplayName();
        dto.reviewType = review.getReviewType();
        dto.passed = review.isPassed();
        dto.demoRequired = review.isDemoRequired();
        dto.demoConfirmed = review.isDemoConfirmed();
        dto.comment = review.getComment();
        dto.createdAt = review.getCreatedAt() == null ? "" : review.getCreatedAt().format(FORMATTER);
        return dto;
    }

    static ReviewerTestEvidenceDTO toEvidenceDTO(ReviewerTestEvidence evidence) {
        ReviewerTestEvidenceDTO dto = new ReviewerTestEvidenceDTO();
        dto.id = evidence.getId();
        dto.itemId = evidence.getItemId();
        dto.componentKey = evidence.getComponentKey();
        dto.reviewerUsername = evidence.getReviewerUsername();
        dto.reviewerDisplayName = evidence.getReviewerDisplayName();
        dto.testType = evidence.getTestType();
        dto.environmentCode = evidence.getEnvironmentCode();
        dto.passed = evidence.isPassed();
        dto.demoObserved = evidence.isDemoObserved();
        dto.responsibilityAccepted = evidence.isResponsibilityAccepted();
        dto.evidence = evidence.getEvidence();
        dto.createdAt = evidence.getCreatedAt() == null ? "" : evidence.getCreatedAt().format(FORMATTER);
        return dto;
    }

    static TestReportDTO toReportDTO(TestReport report) {
        TestReportDTO dto = new TestReportDTO();
        dto.id = report.getId();
        dto.reportType = report.getReportType();
        dto.title = report.getTitle();
        dto.summary = report.getSummary();
        dto.detailJson = report.getDetailJson();
        dto.aiVerdict = report.getAiVerdict();
        dto.passed = report.isPassed();
        dto.createdAt = report.getCreatedAt() == null ? "" : report.getCreatedAt().format(FORMATTER);
        return dto;
    }

    static ReleaseOperationRecordDTO toOperationDTO(ReleaseOperationRecord operation) {
        ReleaseOperationRecordDTO dto = new ReleaseOperationRecordDTO();
        dto.id = operation.getId();
        dto.operationType = operation.getOperationType();
        dto.operationStatus = operation.getOperationStatus();
        dto.environmentCode = operation.getEnvironmentCode();
        dto.targetColor = operation.getTargetColor();
        dto.actorUsername = operation.getActorUsername();
        dto.actorDisplayName = operation.getActorDisplayName();
        dto.safeMode = operation.isSafeMode();
        dto.summary = operation.getSummary();
        dto.detailJson = operation.getDetailJson();
        dto.createdAt = operation.getCreatedAt() == null ? "" : operation.getCreatedAt().format(FORMATTER);
        return dto;
    }
}
