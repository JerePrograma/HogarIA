package com.hogaria.expense.dto;


public record CategoryDto(
        Long id,
        Long familyId,
        String nombre,
        String descripcion,
        Long parentId
) {
}
