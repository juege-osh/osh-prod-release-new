package com.juege.oshrelease.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "demo_record")
public class DemoRecord extends BaseEntity {

    @Column(name = "change_id", nullable = false)
    private Long changeId;

    @Column(name = "developer_username", nullable = false, length = 64)
    private String developerUsername;

    @Column(name = "reviewer_username", nullable = false, length = 64)
    private String reviewerUsername;

    @Column(nullable = false, length = 512)
    private String content;

    public Long getChangeId() {
        return changeId;
    }

    public void setChangeId(Long changeId) {
        this.changeId = changeId;
    }

    public String getDeveloperUsername() {
        return developerUsername;
    }

    public void setDeveloperUsername(String developerUsername) {
        this.developerUsername = developerUsername;
    }

    public String getReviewerUsername() {
        return reviewerUsername;
    }

    public void setReviewerUsername(String reviewerUsername) {
        this.reviewerUsername = reviewerUsername;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

