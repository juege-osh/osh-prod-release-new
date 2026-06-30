package com.juege.oshrelease.dto;

public class ReleaseChangeItemDTO {
    public Long id;
    public String itemKey;
    public int itemOrder;
    public String componentKey;
    public String componentName;
    public String componentType;
    public String ownerUsername;
    public String ownerDisplayName;
    public String title;
    public String itemType;
    public String payloadPath;
    public String changeContent;
    public String executionContent;
    public String incrementalPlan;
    public String rollbackContent;
    public String rollbackPlan;
    public String codeChangeSummary;
    public String riskAnalysis;
    public String bugAnalysis;
    public String verificationCommands;
    public String testPlan;
    public String dataProbePlan;
    public String specStatus;
    public String lifecycleStatus;
    public boolean reviewerAConfirmed;
    public boolean reviewerBConfirmed;
    public boolean juegeConfirmed;
}
