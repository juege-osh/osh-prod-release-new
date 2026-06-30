package com.juege.oshrelease.dto;

public class ReleaseChangeOperationRequest {
    public String reviewerUsername;
    public String reviewerDisplayName;
    public String actorUsername;
    public String actorDisplayName;
    public String comment;
    public boolean passed = true;
    public boolean demoConfirmed = false;
    public boolean safeMode = true;
}
