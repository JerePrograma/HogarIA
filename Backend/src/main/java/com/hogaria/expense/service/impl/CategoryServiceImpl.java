package com.hogaria.expense.service.impl;

import com.hogaria.expense.dto.CategoryDto;
import com.hogaria.expense.model.Category;
import com.hogaria.expense.repository.CategoryRepository;
import com.hogaria.expense.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository repo;

    @Autowired
    public CategoryServiceImpl(CategoryRepository repo) {
        this.repo = repo;
    }

    private static CategoryDto toDto(Category c) {
        return new CategoryDto(
                c.getId(),
                c.getFamilyId(),
                c.getNombre(),
                c.getDescripcion(),
                c.getParentId()
        );
    }

    private static Category toEntity(CategoryDto d) {
        Category c = new Category();
        c.setFamilyId(d.familyId());
        c.setNombre(d.nombre());
        c.setDescripcion(d.descripcion());
        c.setParentId(d.parentId());
        return c;
    }

    @Override
    public List<CategoryDto> getByFamilyId(Long familyId) {
        return repo.findAllByFamilyId(familyId)
                .stream().map(CategoryServiceImpl::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto create(CategoryDto dto) {
        return toDto(repo.save(toEntity(dto)));
    }

    @Override
    public CategoryDto update(Long id, CategoryDto dto) {
        Category existing = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No existe"));
        existing.setNombre(dto.nombre());
        existing.setDescripcion(dto.descripcion());
        existing.setParentId(dto.parentId());
        return toDto(repo.save(existing));
    }

    @Override
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "No existe");
        }
        repo.deleteById(id);
    }
}
