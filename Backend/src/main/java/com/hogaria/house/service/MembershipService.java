package com.hogaria.house.service;

import com.hogaria.house.dto.MembershipCreateDto;
import com.hogaria.house.dto.MembershipDto;
import com.hogaria.house.dto.MembershipUpdateDto;

import java.util.List;

public interface MembershipService {
    List<MembershipDto> getByFamilyId(Long familyId);

    MembershipDto invite(Long familyId, MembershipCreateDto dto);

    MembershipDto updateRole(Long familyId, Long id, MembershipUpdateDto dto);
}
