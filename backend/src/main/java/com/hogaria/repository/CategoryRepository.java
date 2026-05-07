package com.hogaria.repository;
import com.hogaria.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CategoryRepository extends JpaRepository<Category, UUID> { List<Category> findByProfileIdAndActiveTrue(UUID profileId); List<Category> findByScopeAndActiveTrue(Category.Scope scope); Optional<Category> findByIdAndProfileId(UUID id, UUID profileId); Optional<Category> findByIdAndProfileIdIsNull(UUID id); }
