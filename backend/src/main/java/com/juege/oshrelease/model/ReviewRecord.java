package com.juege.oshrelease.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "review_record")
public class ReviewRecord extends BaseEntity {

    @Column(name = "change_id", nullable = false)
    private Long changeId;

    @Column(name = "reviewer_username", nullable = false, length = 64)
    private String reviewerUsername;

    @Column(name = "reviewer_display_name", nullable = false, length = 64)
    private String reviewerDisplayName;

    @Column(name = "review_type", nullable = false, length = 32)
    private String reviewType;

    @Column(nullable = false)
    private boolean passed;

    @Column(name = "demo_required", nullable = false)
    private boolean demoRequired;

    @Column(name = "demo_confirmed", nullable = false)
    private boolean demoConfirmed;

    @Column(nullable = false, length = 512)
    private String comment;

    public Long getChangeId() {
        return changeId;
    }

    public void setChangeId(Long changeId) {
        this.changeId = changeId;
    }

    public String getReviewerUsername() {
        return reviewerUsername;
    }

    public void setReviewerUsername(String reviewerUsername) {
        this.reviewerUsername = reviewerUsername;
    }

    public String getReviewerDisplayName() {
        return reviewerDisplayName;
    }

    public void setReviewerDisplayName(String reviewerDisplayName) {
        this.reviewerDisplayName = reviewerDisplayName;
    }

    public String getReviewType() {
        return reviewType;
    }

    public void setReviewType(String reviewType) {
        this.reviewType = reviewType;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public boolean isDemoRequired() {
        return demoRequired;
    }

    public void setDemoRequired(boolean demoRequired) {
        this.demoRequired = demoRequired;
    }

    public boolean isDemoConfirmed() {
        return demoConfirmed;
    }

    public void setDemoConfirmed(boolean demoConfirmed) {
        this.demoConfirmed = demoConfirmed;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}

