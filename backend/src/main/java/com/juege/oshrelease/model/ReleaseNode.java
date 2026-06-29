package com.juege.oshrelease.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "release_node")
public class ReleaseNode extends BaseEntity {

    @Column(name = "change_id", nullable = false)
    private Long changeId;

    @Column(name = "node_key", nullable = false, length = 64)
    private String nodeKey;

    @Column(name = "component_key", nullable = false, length = 64)
    private String componentKey;

    @Column(name = "component_name", nullable = false, length = 128)
    private String componentName;

    @Column(name = "node_type", nullable = false, length = 32)
    private String nodeType;

    @Column(name = "node_order", nullable = false)
    private int nodeOrder;

    @Column(name = "rollback_order", nullable = false)
    private int rollbackOrder;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "action_type", nullable = false, length = 32)
    private String actionType;

    @Column(name = "host_name", nullable = false, length = 128)
    private String hostName;

    @Column(name = "command_hint", nullable = false, length = 255)
    private String commandHint;

    @Column(name = "config_dir", nullable = false, length = 255)
    private String configDir;

    @Column(name = "data_dir", nullable = false, length = 255)
    private String dataDir;

    @Column(name = "detail_json", nullable = false, columnDefinition = "text")
    private String detailJson;

    public Long getChangeId() {
        return changeId;
    }

    public void setChangeId(Long changeId) {
        this.changeId = changeId;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public void setNodeKey(String nodeKey) {
        this.nodeKey = nodeKey;
    }

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

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public int getNodeOrder() {
        return nodeOrder;
    }

    public void setNodeOrder(int nodeOrder) {
        this.nodeOrder = nodeOrder;
    }

    public int getRollbackOrder() {
        return rollbackOrder;
    }

    public void setRollbackOrder(int rollbackOrder) {
        this.rollbackOrder = rollbackOrder;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getCommandHint() {
        return commandHint;
    }

    public void setCommandHint(String commandHint) {
        this.commandHint = commandHint;
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

    public String getDetailJson() {
        return detailJson;
    }

    public void setDetailJson(String detailJson) {
        this.detailJson = detailJson;
    }
}
