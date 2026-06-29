package com.juege.oshrelease.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "environment")
public class Environment extends BaseEntity {

    @Column(name = "env_code", nullable = false, unique = true, length = 64)
    private String envCode;

    @Column(name = "env_name", nullable = false, length = 128)
    private String envName;

    @Column(name = "env_kind", nullable = false, length = 32)
    private String envKind;

    @Column(name = "base_url", nullable = false, length = 255)
    private String baseUrl;

    @Column(name = "blue_url", length = 255)
    private String blueUrl;

    @Column(name = "green_url", length = 255)
    private String greenUrl;

    @Column(name = "current_color", nullable = false, length = 16)
    private String currentColor;

    @Column(name = "supports_blue_green", nullable = false)
    private boolean supportsBlueGreen;

    @Column(name = "announce_file_exists", nullable = false)
    private boolean announceFileExists;

    @Column(name = "health_status", nullable = false, length = 32)
    private String healthStatus;

    @Column(nullable = false, length = 512)
    private String notes;

    public String getEnvCode() {
        return envCode;
    }

    public void setEnvCode(String envCode) {
        this.envCode = envCode;
    }

    public String getEnvName() {
        return envName;
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    public String getEnvKind() {
        return envKind;
    }

    public void setEnvKind(String envKind) {
        this.envKind = envKind;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getBlueUrl() {
        return blueUrl;
    }

    public void setBlueUrl(String blueUrl) {
        this.blueUrl = blueUrl;
    }

    public String getGreenUrl() {
        return greenUrl;
    }

    public void setGreenUrl(String greenUrl) {
        this.greenUrl = greenUrl;
    }

    public String getCurrentColor() {
        return currentColor;
    }

    public void setCurrentColor(String currentColor) {
        this.currentColor = currentColor;
    }

    public boolean isSupportsBlueGreen() {
        return supportsBlueGreen;
    }

    public void setSupportsBlueGreen(boolean supportsBlueGreen) {
        this.supportsBlueGreen = supportsBlueGreen;
    }

    public boolean isAnnounceFileExists() {
        return announceFileExists;
    }

    public void setAnnounceFileExists(boolean announceFileExists) {
        this.announceFileExists = announceFileExists;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

