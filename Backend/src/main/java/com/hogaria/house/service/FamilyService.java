package com.hogaria.house.service;

import com.hogaria.house.dto.CreateFamilyRequest;
import com.hogaria.house.dto.FamilyDto;

import java.util.List;

public interface FamilyService {
    List<FamilyDto> getByHouseId(Long houseId);

    FamilyDto create(Long houseId, CreateFamilyRequest dto);
}
