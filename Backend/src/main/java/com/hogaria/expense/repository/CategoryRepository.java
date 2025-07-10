package com.hogaria.expense.repository;

import com.hogaria.expense.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByFamilyId(Long familyId);
}
