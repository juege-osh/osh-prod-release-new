package com.juege.oshrelease.dto;

public class ReleaseItemOperationRequest {
    public String actorUsername;
    public String actorDisplayName;
    public String environmentCode;
    public String targetColor;
    public String result;
    public String evidence;
    public boolean passed = true;
    public boolean safeMode = true;
}
