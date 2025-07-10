package com.hogaria.inventory.repository;

import com.hogaria.inventory.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findAllByFamilyId(Long familyId);

    @Query("""
            SELECT i FROM InventoryItem i
             WHERE i.familyId = :familyId
               AND i.quantity <= i.minThreshold
            """)
    List<InventoryItem> findAllUnderThresholdByFamilyId(Long familyId);
}
