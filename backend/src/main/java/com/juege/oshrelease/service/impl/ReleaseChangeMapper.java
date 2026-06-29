package com.juege.oshrelease.service.impl;

import com.juege.oshrelease.dto.ReleaseChangeDetailDTO;
import com.juege.oshrelease.dto.ReleaseChangeListItemDTO;
import com.juege.oshrelease.dto.ReleaseNodeDTO;
import com.juege.oshrelease.dto.ReviewRecordDTO;
import com.juege.oshrelease.dto.TestReportDTO;
import com.juege.oshrelease.model.ReleaseChange;
import com.juege.oshrelease.model.ReleaseNode;
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

    static ReleaseChangeDetailDTO toDetail(ReleaseChange change, List<ReleaseNodeDTO> nodes,
                                           List<ReviewRecordDTO> reviews, List<TestReportDTO> reports) {
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
        dto.reviews = reviews;
        dto.reports = reports;
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
}
