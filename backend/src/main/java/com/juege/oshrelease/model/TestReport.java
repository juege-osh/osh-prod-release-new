package com.juege.oshrelease.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "test_report")
public class TestReport extends BaseEntity {

    @Column(name = "change_id", nullable = false)
    private Long changeId;

    @Column(name = "report_type", nullable = false, length = 32)
    private String reportType;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(nullable = false, length = 512)
    private String summary;

    @Column(name = "detail_json", nullable = false, columnDefinition = "text")
    private String detailJson;

    @Column(name = "ai_verdict", nullable = false, length = 128)
    private String aiVerdict;

    @Column(nullable = false)
    private boolean passed;

    public Long getChangeId() {
        return changeId;
    }

    public void setChangeId(Long changeId) {
        this.changeId = changeId;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDetailJson() {
        return detailJson;
    }

    public void setDetailJson(String detailJson) {
        this.detailJson = detailJson;
    }

    public String getAiVerdict() {
        return aiVerdict;
    }

    public void setAiVerdict(String aiVerdict) {
        this.aiVerdict = aiVerdict;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }
}
