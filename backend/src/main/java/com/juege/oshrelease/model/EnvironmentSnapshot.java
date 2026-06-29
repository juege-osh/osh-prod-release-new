package com.juege.oshrelease.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "environment_snapshot")
public class EnvironmentSnapshot extends BaseEntity {

    @Column(name = "env_code", nullable = false, length = 64)
    private String envCode;

    @Column(name = "snapshot_name", nullable = false, length = 128)
    private String snapshotName;

    @Column(name = "snapshot_type", nullable = false, length = 32)
    private String snapshotType;

    @Column(name = "summary_json", nullable = false, columnDefinition = "text")
    private String summaryJson;

    public String getEnvCode() {
        return envCode;
    }

    public void setEnvCode(String envCode) {
        this.envCode = envCode;
    }

    public String getSnapshotName() {
        return snapshotName;
    }

    public void setSnapshotName(String snapshotName) {
        this.snapshotName = snapshotName;
    }

    public String getSnapshotType() {
        return snapshotType;
    }

    public void setSnapshotType(String snapshotType) {
        this.snapshotType = snapshotType;
    }

    public String getSummaryJson() {
        return summaryJson;
    }

    public void setSummaryJson(String summaryJson) {
        this.summaryJson = summaryJson;
    }
}
