package com.hogaria.house.service.impl;

import com.hogaria.house.dto.CreateHouseRequest;
import com.hogaria.house.dto.HouseDto;
import com.hogaria.house.model.House;
import com.hogaria.house.repository.HouseRepository;
import com.hogaria.house.service.HouseService;
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
public class HouseServiceImpl implements HouseService {

    private final HouseRepository repo;

    @Override
    public List<HouseDto> listAll() {
        return repo.findAll()
                .stream()
                .map(h -> new HouseDto(h.getId(), h.getNombre(), h.getDireccion()))
                .collect(Collectors.toList());
    }

    @Override
    public HouseDto create(CreateHouseRequest dto) {
        House h = new House();
        h.setNombre(dto.nombre());
        h.setDireccion(dto.direccion());
        House saved = repo.save(h);
        return new HouseDto(saved.getId(), saved.getNombre(), saved.getDireccion());
    }
}
