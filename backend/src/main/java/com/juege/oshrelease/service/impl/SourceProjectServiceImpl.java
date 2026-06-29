package com.juege.oshrelease.service.impl;

import com.juege.oshrelease.dto.SourceProjectDTO;
import com.juege.oshrelease.model.SourceProject;
import com.juege.oshrelease.repo.SourceProjectRepository;
import com.juege.oshrelease.service.SourceProjectService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SourceProjectServiceImpl implements SourceProjectService {

    private final SourceProjectRepository sourceProjectRepository;

    public SourceProjectServiceImpl(SourceProjectRepository sourceProjectRepository) {
        this.sourceProjectRepository = sourceProjectRepository;
    }

    @Override
    public List<SourceProjectDTO> list() {
        List<SourceProject> entities = sourceProjectRepository.findAll();
        entities.sort(Comparator.comparing(SourceProject::getProjectKey));
        List<SourceProjectDTO> result = new ArrayList<SourceProjectDTO>();
        for (SourceProject entity : entities) {
            result.add(BaseDataMapper.toSourceProjectDTO(entity));
        }
        return result;
    }
}
