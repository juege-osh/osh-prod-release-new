package com.juege.oshrelease.dto;

public class ReviewerTestEvidenceRequest {
    public Long itemId;
    public String componentKey;
    public String reviewerUsername;
    public String reviewerDisplayName;
    public String testType;
    public String environmentCode;
    public boolean passed = true;
    public boolean demoObserved = false;
    public boolean responsibilityAccepted = true;
    public String evidence;
}
