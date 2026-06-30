package com.juege.oshrelease.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "release_change_item")
public class ReleaseChangeItem extends BaseEntity {

    @Column(name = "change_id", nullable = false)
    private Long changeId;

    @Column(name = "item_key", nullable = false, length = 96)
    private String itemKey;

    @Column(name = "item_order", nullable = false)
    private int itemOrder;

    @Column(name = "component_key", nullable = false, length = 64)
    private String componentKey;

    @Column(name = "component_name", nullable = false, length = 128)
    private String componentName;

    @Column(name = "component_type", nullable = false, length = 32)
    private String componentType;

    @Column(name = "owner_username", nullable = false, length = 64)
    private String ownerUsername;

    @Column(name = "owner_display_name", nullable = false, length = 64)
    private String ownerDisplayName;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(name = "item_type", nullable = false, length = 32)
    private String itemType;

    @Column(name = "payload_path", nullable = false, length = 512)
    private String payloadPath;

    @Column(name = "change_content", nullable = false, columnDefinition = "text")
    private String changeContent;

    @Column(name = "execution_content", nullable = false, columnDefinition = "text")
    private String executionContent;

    @Column(name = "incremental_plan", nullable = false, columnDefinition = "text")
    private String incrementalPlan;

    @Column(name = "rollback_content", nullable = false, columnDefinition = "text")
    private String rollbackContent;

    @Column(name = "rollback_plan", nullable = false, columnDefinition = "text")
    private String rollbackPlan;

    @Column(name = "code_change_summary", nullable = false, columnDefinition = "text")
    private String codeChangeSummary;

    @Column(name = "risk_analysis", nullable = false, columnDefinition = "text")
    private String riskAnalysis;

    @Column(name = "bug_analysis", nullable = false, columnDefinition = "text")
    private String bugAnalysis;

    @Column(name = "verification_commands", nullable = false, columnDefinition = "text")
    private String verificationCommands;

    @Column(name = "test_plan", nullable = false, columnDefinition = "text")
    private String testPlan;

    @Column(name = "data_probe_plan", nullable = false, columnDefinition = "text")
    private String dataProbePlan;

    @Column(name = "spec_status", nullable = false, length = 32)
    private String specStatus;

    @Column(name = "lifecycle_status", nullable = false, length = 32)
    private String lifecycleStatus;

    @Column(name = "reviewer_a_confirmed", nullable = false)
    private boolean reviewerAConfirmed;

    @Column(name = "reviewer_b_confirmed", nullable = false)
    private boolean reviewerBConfirmed;

    @Column(name = "juege_confirmed", nullable = false)
    private boolean juegeConfirmed;

    public Long getChangeId() {
        return changeId;
    }

    public void setChangeId(Long changeId) {
        this.changeId = changeId;
    }

    public String getItemKey() {
        return itemKey;
    }

    public void setItemKey(String itemKey) {
        this.itemKey = itemKey;
    }

    public int getItemOrder() {
        return itemOrder;
    }

    public void setItemOrder(int itemOrder) {
        this.itemOrder = itemOrder;
    }

    public String getComponentKey() {
        return componentKey;
    }

    public void setComponentKey(String componentKey) {
        this.componentKey = componentKey;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getComponentType() {
        return componentType;
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getOwnerDisplayName() {
        return ownerDisplayName;
    }

    public void setOwnerDisplayName(String ownerDisplayName) {
        this.ownerDisplayName = ownerDisplayName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getPayloadPath() {
        return payloadPath;
    }

    public void setPayloadPath(String payloadPath) {
        this.payloadPath = payloadPath;
    }

    public String getChangeContent() {
        return changeContent;
    }

    public void setChangeContent(String changeContent) {
        this.changeContent = changeContent;
    }

    public String getExecutionContent() {
        return executionContent;
    }

    public void setExecutionContent(String executionContent) {
        this.executionContent = executionContent;
    }

    public String getIncrementalPlan() {
        return incrementalPlan;
    }

    public void setIncrementalPlan(String incrementalPlan) {
        this.incrementalPlan = incrementalPlan;
    }

    public String getRollbackContent() {
        return rollbackContent;
    }

    public void setRollbackContent(String rollbackContent) {
        this.rollbackContent = rollbackContent;
    }

    public String getRollbackPlan() {
        return rollbackPlan;
    }

    public void setRollbackPlan(String rollbackPlan) {
        this.rollbackPlan = rollbackPlan;
    }

    public String getCodeChangeSummary() {
        return codeChangeSummary;
    }

    public void setCodeChangeSummary(String codeChangeSummary) {
        this.codeChangeSummary = codeChangeSummary;
    }

    public String getRiskAnalysis() {
        return riskAnalysis;
    }

    public void setRiskAnalysis(String riskAnalysis) {
        this.riskAnalysis = riskAnalysis;
    }

    public String getBugAnalysis() {
        return bugAnalysis;
    }

    public void setBugAnalysis(String bugAnalysis) {
        this.bugAnalysis = bugAnalysis;
    }

    public String getVerificationCommands() {
        return verificationCommands;
    }

    public void setVerificationCommands(String verificationCommands) {
        this.verificationCommands = verificationCommands;
    }

    public String getTestPlan() {
        return testPlan;
    }

    public void setTestPlan(String testPlan) {
        this.testPlan = testPlan;
    }

    public String getDataProbePlan() {
        return dataProbePlan;
    }

    public void setDataProbePlan(String dataProbePlan) {
        this.dataProbePlan = dataProbePlan;
    }

    public String getSpecStatus() {
        return specStatus;
    }

    public void setSpecStatus(String specStatus) {
        this.specStatus = specStatus;
    }

    public String getLifecycleStatus() {
        return lifecycleStatus;
    }

    public void setLifecycleStatus(String lifecycleStatus) {
        this.lifecycleStatus = lifecycleStatus;
    }

    public boolean isReviewerAConfirmed() {
        return reviewerAConfirmed;
    }

    public void setReviewerAConfirmed(boolean reviewerAConfirmed) {
        this.reviewerAConfirmed = reviewerAConfirmed;
    }

    public boolean isReviewerBConfirmed() {
        return reviewerBConfirmed;
    }

    public void setReviewerBConfirmed(boolean reviewerBConfirmed) {
        this.reviewerBConfirmed = reviewerBConfirmed;
    }

    public boolean isJuegeConfirmed() {
        return juegeConfirmed;
    }

    public void setJuegeConfirmed(boolean juegeConfirmed) {
        this.juegeConfirmed = juegeConfirmed;
    }
}
