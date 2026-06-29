package com.juege.oshrelease.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "source_project")
public class SourceProject extends BaseEntity {

    @Column(name = "project_key", nullable = false, unique = true, length = 64)
    private String projectKey;

    @Column(name = "project_name", nullable = false, length = 128)
    private String projectName;

    @Column(name = "repo_url", nullable = false, length = 255)
    private String repoUrl;

    @Column(name = "release_branch", nullable = false, length = 128)
    private String releaseBranch;

    @Column(name = "release_commit", nullable = false, length = 64)
    private String releaseCommit;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false, length = 512)
    private String summary;

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public String getReleaseBranch() {
        return releaseBranch;
    }

    public void setReleaseBranch(String releaseBranch) {
        this.releaseBranch = releaseBranch;
    }

    public String getReleaseCommit() {
        return releaseCommit;
    }

    public void setReleaseCommit(String releaseCommit) {
        this.releaseCommit = releaseCommit;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}

