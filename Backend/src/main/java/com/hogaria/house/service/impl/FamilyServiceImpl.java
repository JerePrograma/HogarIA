package com.hogaria.house.service.impl;

import com.hogaria.house.dto.CreateFamilyRequest;
import com.hogaria.house.dto.FamilyDto;
import com.hogaria.house.model.Family;
import com.hogaria.house.model.House;
import com.hogaria.house.repository.FamilyRepository;
import com.hogaria.house.repository.HouseRepository;
import com.hogaria.house.service.FamilyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class FamilyServiceImpl implements FamilyService {

    private final FamilyRepository familyRepo;
    private final HouseRepository houseRepo;

    @Override
    public List<FamilyDto> getByHouseId(Long houseId) {
        return familyRepo.findAllByHouse_Id(houseId)
                .stream()
                .map(f -> new FamilyDto(f.getId(), f.getHouse().getId(), f.getNombre(), f.getDescripcion()))
                .collect(Collectors.toList());
    }

    @Override
    public FamilyDto create(Long houseId, CreateFamilyRequest dto) {
        House h = houseRepo.findById(houseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "House not found"));
        Family f = new Family();
        f.setHouse(h);
        f.setNombre(dto.nombre());
        f.setDescripcion(dto.descripcion());
        Family saved = familyRepo.save(f);
        return new FamilyDto(saved.getId(), saved.getHouse().getId(), saved.getNombre(), saved.getDescripcion());
    }
}
