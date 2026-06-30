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
    public String changeContent;
    public String incrementalPlan;
    public String rollbackPlan;
    public String testPlan;
    public String dataProbePlan;
    public String specStatus;
    public String lifecycleStatus;
    public boolean reviewerAConfirmed;
    public boolean reviewerBConfirmed;
    public boolean juegeConfirmed;
}
