package com.hogaria.expense.service;

import com.hogaria.expense.dto.CategoryDto;

import java.util.List;

public interface CategoryService {
    List<CategoryDto> getByFamilyId(Long familyId);

    CategoryDto create(CategoryDto dto);

    CategoryDto update(Long id, CategoryDto dto);

    void delete(Long id);
}
