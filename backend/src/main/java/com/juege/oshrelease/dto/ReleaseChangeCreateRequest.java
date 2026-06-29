package com.juege.oshrelease.dto;

import com.juege.oshrelease.common.ReleaseType;
import java.util.List;

public class ReleaseChangeCreateRequest {
    public String title;
    public String projectBranch;
    public ReleaseType releaseType;
    public String targetEnvCode;
    public String targetColor;
    public String developerUsername;
    public String developerDisplayName;
    public String reviewMode;
    public boolean demoRequired;
    public String riskLevel;
    public String summary;
    public List<String> componentKeys;
    public String contentJson;
}

