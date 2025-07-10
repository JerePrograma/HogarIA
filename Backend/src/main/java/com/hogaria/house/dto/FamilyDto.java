package com.hogaria.house.dto;


public record FamilyDto(
        Long id,
        Long houseId,
        String nombre,
        String descripcion
) {
}
