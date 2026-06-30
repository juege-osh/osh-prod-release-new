package com.juege.oshrelease.service.impl;

import com.juege.oshrelease.dto.ComponentDTO;
import com.juege.oshrelease.dto.EnvironmentDTO;
import com.juege.oshrelease.dto.SourceProjectDTO;
import com.juege.oshrelease.model.ComponentDefinition;
import com.juege.oshrelease.model.Environment;
import com.juege.oshrelease.model.SourceProject;

final class BaseDataMapper {

    private BaseDataMapper() {
    }

    static EnvironmentDTO toEnvironmentDTO(Environment entity) {
        EnvironmentDTO dto = new EnvironmentDTO();
        dto.id = entity.getId();
        dto.envCode = entity.getEnvCode();
        dto.envName = entity.getEnvName();
        dto.envKind = entity.getEnvKind();
        dto.baseUrl = entity.getBaseUrl();
        dto.blueUrl = entity.getBlueUrl();
        dto.greenUrl = entity.getGreenUrl();
        dto.currentColor = entity.getCurrentColor();
        dto.supportsBlueGreen = entity.isSupportsBlueGreen();
        dto.announceFileExists = entity.isAnnounceFileExists();
        dto.healthStatus = entity.getHealthStatus();
        dto.notes = entity.getNotes();
        return dto;
    }

    static ComponentDTO toComponentDTO(ComponentDefinition entity) {
        ComponentDTO dto = new ComponentDTO();
        dto.id = entity.getId();
        dto.componentKey = entity.getComponentKey();
        dto.componentName = entity.getComponentName();
        dto.componentType = entity.getComponentType();
        dto.configDir = entity.getConfigDir();
        dto.dataDir = entity.getDataDir();
        dto.deployPath = entity.getDeployPath();
        dto.supportIncremental = entity.isSupportIncremental();
        dto.supportRollback = entity.isSupportRollback();
        dto.supportBlueGreen = entity.isSupportBlueGreen();
        dto.extension = entity.isExtension();
        dto.core = entity.isCore();
        dto.installOrder = entity.getInstallOrder();
        dto.rollbackOrder = entity.getRollbackOrder();
        dto.actionTypes = entity.getActionTypes();
        dto.observedStatus = entity.getObservedStatus();
        dto.runtimeInventory = entity.getRuntimeInventory();
        dto.notes = entity.getNotes();
        return dto;
    }

    static SourceProjectDTO toSourceProjectDTO(SourceProject entity) {
        SourceProjectDTO dto = new SourceProjectDTO();
        dto.id = entity.getId();
        dto.projectKey = entity.getProjectKey();
        dto.projectName = entity.getProjectName();
        dto.repoUrl = entity.getRepoUrl();
        dto.releaseBranch = entity.getReleaseBranch();
        dto.releaseCommit = entity.getReleaseCommit();
        dto.sourceType = entity.getSourceType();
        dto.status = entity.getStatus();
        dto.summary = entity.getSummary();
        return dto;
    }
}
