package com.juege.oshrelease.service.impl;

import com.juege.oshrelease.dto.ComponentDTO;
import com.juege.oshrelease.model.ComponentDefinition;
import com.juege.oshrelease.repo.ComponentRepository;
import com.juege.oshrelease.service.ComponentService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ComponentServiceImpl implements ComponentService {

    private final ComponentRepository componentRepository;

    public ComponentServiceImpl(ComponentRepository componentRepository) {
        this.componentRepository = componentRepository;
    }

    @Override
    public List<ComponentDTO> list() {
        List<ComponentDefinition> entities = componentRepository.findAll();
        entities.sort(Comparator.comparingInt(ComponentDefinition::getInstallOrder));
        List<ComponentDTO> result = new ArrayList<ComponentDTO>();
        for (ComponentDefinition entity : entities) {
            result.add(BaseDataMapper.toComponentDTO(entity));
        }
        return result;
    }
}
