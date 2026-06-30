package com.juege.oshrelease.dto;

import java.util.List;

public class ReleaseChangeDetailDTO {
    public Long id;
    public String changeCode;
    public String title;
    public String projectBranch;
    public String releaseType;
    public String targetEnvCode;
    public String targetColor;
    public String status;
    public String developerUsername;
    public String developerDisplayName;
    public String reviewerAUsername;
    public String reviewerBUsername;
    public String reviewMode;
    public boolean demoRequired;
    public boolean demoConfirmed;
    public String riskLevel;
    public String summary;
    public String contentJson;
    public String currentStep;
    public String finalMessage;
    public List<ReleaseNodeDTO> nodes;
    public List<ReleaseChangeItemDTO> items;
    public List<ReviewRecordDTO> reviews;
    public List<ReviewerTestEvidenceDTO> evidences;
    public List<TestReportDTO> reports;
    public List<ReleaseOperationRecordDTO> operations;
}
