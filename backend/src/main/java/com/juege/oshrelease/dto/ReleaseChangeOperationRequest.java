package com.juege.oshrelease.dto;

public class ReleaseChangeOperationRequest {
    public String reviewerUsername;
    public String reviewerDisplayName;
    public String comment;
    public boolean passed = true;
    public boolean demoConfirmed = false;
}

