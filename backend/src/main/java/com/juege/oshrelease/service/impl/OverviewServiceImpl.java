package com.juege.oshrelease.service.impl;

import com.juege.oshrelease.common.ChangeStatus;
import com.juege.oshrelease.model.Environment;
import com.juege.oshrelease.model.ReleaseChange;
import com.juege.oshrelease.repo.ComponentRepository;
import com.juege.oshrelease.repo.EnvironmentRepository;
import com.juege.oshrelease.repo.ReleaseChangeRepository;
import com.juege.oshrelease.repo.SourceProjectRepository;
import com.juege.oshrelease.service.OverviewService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OverviewServiceImpl implements OverviewService {

    private final ReleaseChangeRepository releaseChangeRepository;
    private final EnvironmentRepository environmentRepository;
    private final ComponentRepository componentRepository;
    private final SourceProjectRepository sourceProjectRepository;

    public OverviewServiceImpl(ReleaseChangeRepository releaseChangeRepository,
                               EnvironmentRepository environmentRepository,
                               ComponentRepository componentRepository,
                               SourceProjectRepository sourceProjectRepository) {
        this.releaseChangeRepository = releaseChangeRepository;
        this.environmentRepository = environmentRepository;
        this.componentRepository = componentRepository;
        this.sourceProjectRepository = sourceProjectRepository;
    }

    @Override
    public Map<String, Object> summary() {
        List<ReleaseChange> changes = releaseChangeRepository.findAll();
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("changeCount", changes.size());
        result.put("componentCount", componentRepository.count());
        result.put("sourceProjectCount", sourceProjectRepository.count());
        result.put("activeChangeCount", countActive(changes));
        result.put("releasedCount", countStatus(changes, ChangeStatus.RELEASED));
        result.put("rolledBackCount", countStatus(changes, ChangeStatus.ROLLED_BACK));
        result.put("riskGuard", "生产写操作默认禁止，必须觉哥显式确认。");
        result.put("releaseBranch", "release/20260708");
        result.put("environments", environments());
        return result;
    }

    private long countActive(List<ReleaseChange> changes) {
        long count = 0;
        for (ReleaseChange change : changes) {
            if (change.getStatus() != ChangeStatus.RELEASED
                    && change.getStatus() != ChangeStatus.ROLLED_BACK
                    && change.getStatus() != ChangeStatus.REJECTED) {
                count++;
            }
        }
        return count;
    }

    private long countStatus(List<ReleaseChange> changes, ChangeStatus status) {
        long count = 0;
        for (ReleaseChange change : changes) {
            if (change.getStatus() == status) {
                count++;
            }
        }
        return count;
    }

    private List<Map<String, Object>> environments() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Environment env : environmentRepository.findAll()) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("envCode", env.getEnvCode());
            item.put("envName", env.getEnvName());
            item.put("baseUrl", env.getBaseUrl());
            item.put("currentColor", env.getCurrentColor());
            item.put("healthStatus", env.getHealthStatus());
            item.put("announceFileExists", env.isAnnounceFileExists());
            result.add(item);
        }
        return result;
    }
}
