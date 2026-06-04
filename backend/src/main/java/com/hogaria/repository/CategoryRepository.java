package com.hogaria.repository;
import com.hogaria.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByProfileIdAndActiveTrue(UUID profileId);
    List<Category> findByProfileId(UUID profileId);
    List<Category> findByProfileIdIsNullAndActiveTrue();
    List<Category> findByProfileIdIsNull();
    Optional<Category> findByIdAndProfileId(UUID id, UUID profileId);
    Optional<Category> findByIdAndProfileIdIsNull(UUID id);

    @Query("""
            select count(c) > 0 from Category c
            where c.profileId = :profileId
              and c.categoryKey = :categoryKey
              and c.type = :type
              and c.active = true
              and (:excludeId is null or c.id <> :excludeId)
            """)
    boolean existsActiveProfileDuplicateKey(
            @Param("profileId") UUID profileId,
            @Param("categoryKey") String categoryKey,
            @Param("type") Category.Type type,
            @Param("excludeId") UUID excludeId
    );

    @Query("""
            select count(c) > 0 from Category c
            where c.profileId is null
              and c.categoryKey = :categoryKey
              and c.type = :type
              and c.active = true
              and (:excludeId is null or c.id <> :excludeId)
            """)
    boolean existsActiveGlobalDuplicateKey(
            @Param("categoryKey") String categoryKey,
            @Param("type") Category.Type type,
            @Param("excludeId") UUID excludeId
    );
}
