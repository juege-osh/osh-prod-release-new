package com.juege.oshrelease.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "reviewer_test_evidence")
public class ReviewerTestEvidence extends BaseEntity {

    @Column(name = "change_id", nullable = false)
    private Long changeId;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "component_key", nullable = false, length = 64)
    private String componentKey;

    @Column(name = "reviewer_username", nullable = false, length = 64)
    private String reviewerUsername;

    @Column(name = "reviewer_display_name", nullable = false, length = 64)
    private String reviewerDisplayName;

    @Column(name = "test_type", nullable = false, length = 32)
    private String testType;

    @Column(name = "environment_code", nullable = false, length = 64)
    private String environmentCode;

    @Column(nullable = false)
    private boolean passed;

    @Column(name = "demo_observed", nullable = false)
    private boolean demoObserved;

    @Column(name = "responsibility_accepted", nullable = false)
    private boolean responsibilityAccepted;

    @Column(nullable = false, columnDefinition = "text")
    private String evidence;

    public Long getChangeId() {
        return changeId;
    }

    public void setChangeId(Long changeId) {
        this.changeId = changeId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getComponentKey() {
        return componentKey;
    }

    public void setComponentKey(String componentKey) {
        this.componentKey = componentKey;
    }

    public String getReviewerUsername() {
        return reviewerUsername;
    }

    public void setReviewerUsername(String reviewerUsername) {
        this.reviewerUsername = reviewerUsername;
    }

    public String getReviewerDisplayName() {
        return reviewerDisplayName;
    }

    public void setReviewerDisplayName(String reviewerDisplayName) {
        this.reviewerDisplayName = reviewerDisplayName;
    }

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = testType;
    }

    public String getEnvironmentCode() {
        return environmentCode;
    }

    public void setEnvironmentCode(String environmentCode) {
        this.environmentCode = environmentCode;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public boolean isDemoObserved() {
        return demoObserved;
    }

    public void setDemoObserved(boolean demoObserved) {
        this.demoObserved = demoObserved;
    }

    public boolean isResponsibilityAccepted() {
        return responsibilityAccepted;
    }

    public void setResponsibilityAccepted(boolean responsibilityAccepted) {
        this.responsibilityAccepted = responsibilityAccepted;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }
}
