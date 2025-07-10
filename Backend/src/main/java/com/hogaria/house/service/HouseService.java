package com.hogaria.house.service;

import com.hogaria.house.dto.CreateHouseRequest;
import com.hogaria.house.dto.HouseDto;

import java.util.List;

public interface HouseService {
    List<HouseDto> listAll();

    HouseDto create(CreateHouseRequest dto);
}
