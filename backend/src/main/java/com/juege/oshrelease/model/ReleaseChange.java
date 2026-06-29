package com.juege.oshrelease.model;

import com.juege.oshrelease.common.ChangeStatus;
import com.juege.oshrelease.common.ReleaseType;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

@Entity
@Table(name = "release_change")
public class ReleaseChange extends BaseEntity {

    @Column(name = "change_code", nullable = false, unique = true, length = 64)
    private String changeCode;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(name = "project_branch", nullable = false, length = 128)
    private String projectBranch;

    @Enumerated(EnumType.STRING)
    @Column(name = "release_type", nullable = false, length = 32)
    private ReleaseType releaseType;

    @Column(name = "target_env_code", nullable = false, length = 64)
    private String targetEnvCode;

    @Column(name = "target_color", nullable = false, length = 16)
    private String targetColor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ChangeStatus status;

    @Column(name = "developer_username", nullable = false, length = 64)
    private String developerUsername;

    @Column(name = "developer_display_name", nullable = false, length = 64)
    private String developerDisplayName;

    @Column(name = "reviewer_a_username", length = 64)
    private String reviewerAUsername;

    @Column(name = "reviewer_b_username", length = 64)
    private String reviewerBUsername;

    @Column(name = "review_mode", nullable = false, length = 32)
    private String reviewMode;

    @Column(name = "demo_required", nullable = false)
    private boolean demoRequired;

    @Column(name = "demo_confirmed", nullable = false)
    private boolean demoConfirmed;

    @Column(name = "risk_level", nullable = false, length = 32)
    private String riskLevel;

    @Column(nullable = false, length = 512)
    private String summary;

    @Column(name = "content_json", nullable = false, columnDefinition = "text")
    private String contentJson;

    @Column(name = "current_step", nullable = false, length = 64)
    private String currentStep;

    @Column(name = "final_message", nullable = false, length = 512)
    private String finalMessage;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "switched_at")
    private LocalDateTime switchedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "rolled_back_at")
    private LocalDateTime rolledBackAt;

    public String getChangeCode() {
        return changeCode;
    }

    public void setChangeCode(String changeCode) {
        this.changeCode = changeCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getProjectBranch() {
        return projectBranch;
    }

    public void setProjectBranch(String projectBranch) {
        this.projectBranch = projectBranch;
    }

    public ReleaseType getReleaseType() {
        return releaseType;
    }

    public void setReleaseType(ReleaseType releaseType) {
        this.releaseType = releaseType;
    }

    public String getTargetEnvCode() {
        return targetEnvCode;
    }

    public void setTargetEnvCode(String targetEnvCode) {
        this.targetEnvCode = targetEnvCode;
    }

    public String getTargetColor() {
        return targetColor;
    }

    public void setTargetColor(String targetColor) {
        this.targetColor = targetColor;
    }

    public ChangeStatus getStatus() {
        return status;
    }

    public void setStatus(ChangeStatus status) {
        this.status = status;
    }

    public String getDeveloperUsername() {
        return developerUsername;
    }

    public void setDeveloperUsername(String developerUsername) {
        this.developerUsername = developerUsername;
    }

    public String getDeveloperDisplayName() {
        return developerDisplayName;
    }

    public void setDeveloperDisplayName(String developerDisplayName) {
        this.developerDisplayName = developerDisplayName;
    }

    public String getReviewerAUsername() {
        return reviewerAUsername;
    }

    public void setReviewerAUsername(String reviewerAUsername) {
        this.reviewerAUsername = reviewerAUsername;
    }

    public String getReviewerBUsername() {
        return reviewerBUsername;
    }

    public void setReviewerBUsername(String reviewerBUsername) {
        this.reviewerBUsername = reviewerBUsername;
    }

    public String getReviewMode() {
        return reviewMode;
    }

    public void setReviewMode(String reviewMode) {
        this.reviewMode = reviewMode;
    }

    public boolean isDemoRequired() {
        return demoRequired;
    }

    public void setDemoRequired(boolean demoRequired) {
        this.demoRequired = demoRequired;
    }

    public boolean isDemoConfirmed() {
        return demoConfirmed;
    }

    public void setDemoConfirmed(boolean demoConfirmed) {
        this.demoConfirmed = demoConfirmed;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContentJson() {
        return contentJson;
    }

    public void setContentJson(String contentJson) {
        this.contentJson = contentJson;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public String getFinalMessage() {
        return finalMessage;
    }

    public void setFinalMessage(String finalMessage) {
        this.finalMessage = finalMessage;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public LocalDateTime getSwitchedAt() {
        return switchedAt;
    }

    public void setSwitchedAt(LocalDateTime switchedAt) {
        this.switchedAt = switchedAt;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(LocalDateTime releasedAt) {
        this.releasedAt = releasedAt;
    }

    public LocalDateTime getRolledBackAt() {
        return rolledBackAt;
    }

    public void setRolledBackAt(LocalDateTime rolledBackAt) {
        this.rolledBackAt = rolledBackAt;
    }
}
