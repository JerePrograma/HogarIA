package com.hogaria.inventory.dto;


public record UnitDto(
        Long id,
        String codigo,
        String descripcion,
        boolean isCustom
) {
}
