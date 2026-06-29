package com.juege.oshrelease.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "component")
public class ComponentDefinition extends BaseEntity {

    @Column(name = "component_key", nullable = false, unique = true, length = 64)
    private String componentKey;

    @Column(name = "component_name", nullable = false, length = 128)
    private String componentName;

    @Column(name = "component_type", nullable = false, length = 32)
    private String componentType;

    @Column(name = "config_dir", nullable = false, length = 255)
    private String configDir;

    @Column(name = "data_dir", nullable = false, length = 255)
    private String dataDir;

    @Column(name = "deploy_path", nullable = false, length = 255)
    private String deployPath;

    @Column(name = "support_incremental", nullable = false)
    private boolean supportIncremental;

    @Column(name = "support_rollback", nullable = false)
    private boolean supportRollback;

    @Column(name = "support_blue_green", nullable = false)
    private boolean supportBlueGreen;

    @Column(name = "is_extension", nullable = false)
    private boolean extension;

    @Column(name = "is_core", nullable = false)
    private boolean core;

    @Column(name = "install_order", nullable = false)
    private int installOrder;

    @Column(name = "rollback_order", nullable = false)
    private int rollbackOrder;

    @Column(nullable = false, length = 512)
    private String notes;

    public String getComponentKey() {
        return componentKey;
    }

    public void setComponentKey(String componentKey) {
        this.componentKey = componentKey;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getComponentType() {
        return componentType;
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    public String getConfigDir() {
        return configDir;
    }

    public void setConfigDir(String configDir) {
        this.configDir = configDir;
    }

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public String getDeployPath() {
        return deployPath;
    }

    public void setDeployPath(String deployPath) {
        this.deployPath = deployPath;
    }

    public boolean isSupportIncremental() {
        return supportIncremental;
    }

    public void setSupportIncremental(boolean supportIncremental) {
        this.supportIncremental = supportIncremental;
    }

    public boolean isSupportRollback() {
        return supportRollback;
    }

    public void setSupportRollback(boolean supportRollback) {
        this.supportRollback = supportRollback;
    }

    public boolean isSupportBlueGreen() {
        return supportBlueGreen;
    }

    public void setSupportBlueGreen(boolean supportBlueGreen) {
        this.supportBlueGreen = supportBlueGreen;
    }

    public boolean isExtension() {
        return extension;
    }

    public void setExtension(boolean extension) {
        this.extension = extension;
    }

    public boolean isCore() {
        return core;
    }

    public void setCore(boolean core) {
        this.core = core;
    }

    public int getInstallOrder() {
        return installOrder;
    }

    public void setInstallOrder(int installOrder) {
        this.installOrder = installOrder;
    }

    public int getRollbackOrder() {
        return rollbackOrder;
    }

    public void setRollbackOrder(int rollbackOrder) {
        this.rollbackOrder = rollbackOrder;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

