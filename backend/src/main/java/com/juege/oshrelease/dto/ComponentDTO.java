package com.juege.oshrelease.dto;

public class ComponentDTO {
    public Long id;
    public String componentKey;
    public String componentName;
    public String componentType;
    public String configDir;
    public String dataDir;
    public String deployPath;
    public boolean supportIncremental;
    public boolean supportRollback;
    public boolean supportBlueGreen;
    public boolean extension;
    public boolean core;
    public int installOrder;
    public int rollbackOrder;
    public String actionTypes;
    public String observedStatus;
    public String runtimeInventory;
    public String notes;
}
