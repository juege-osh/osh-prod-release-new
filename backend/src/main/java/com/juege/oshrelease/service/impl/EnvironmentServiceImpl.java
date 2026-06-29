package com.juege.oshrelease.service.impl;

import com.juege.oshrelease.dto.EnvironmentDTO;
import com.juege.oshrelease.model.Environment;
import com.juege.oshrelease.repo.EnvironmentRepository;
import com.juege.oshrelease.service.EnvironmentService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EnvironmentServiceImpl implements EnvironmentService {

    private final EnvironmentRepository environmentRepository;

    public EnvironmentServiceImpl(EnvironmentRepository environmentRepository) {
        this.environmentRepository = environmentRepository;
    }

    @Override
    public List<EnvironmentDTO> list() {
        List<Environment> entities = environmentRepository.findAll();
        entities.sort(Comparator.comparing(Environment::getEnvCode));
        List<EnvironmentDTO> result = new ArrayList<EnvironmentDTO>();
        for (Environment entity : entities) {
            result.add(BaseDataMapper.toEnvironmentDTO(entity));
        }
        return result;
    }
}
