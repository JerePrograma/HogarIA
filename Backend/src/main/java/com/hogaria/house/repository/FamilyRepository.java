package com.hogaria.house.repository;

import com.hogaria.house.model.Family;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FamilyRepository extends JpaRepository<Family, Long> {
    List<Family> findAllByHouse_Id(Long houseId);
}
