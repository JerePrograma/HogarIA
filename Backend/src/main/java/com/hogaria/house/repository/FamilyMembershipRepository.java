package com.hogaria.house.repository;

import com.hogaria.house.model.FamilyMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FamilyMembershipRepository extends JpaRepository<FamilyMembership, Long> {
    List<FamilyMembership> findAllByFamily_Id(Long familyId);
}
