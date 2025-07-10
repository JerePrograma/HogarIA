package com.hogaria.house.service.impl;

import com.hogaria.house.dto.MembershipCreateDto;
import com.hogaria.house.dto.MembershipDto;
import com.hogaria.house.dto.MembershipUpdateDto;
import com.hogaria.house.model.*;
import com.hogaria.house.repository.FamilyMembershipRepository;
import com.hogaria.house.repository.FamilyRepository;
import com.hogaria.house.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {

    private final FamilyMembershipRepository repo;
    private final FamilyRepository familyRepo;

    private static MembershipDto toDto(FamilyMembership m) {
        return new MembershipDto(
                m.getId(),
                m.getUserId(),
                m.getFamily().getId(),
                m.getRole().name(),
                m.getJoinedAt()
        );
    }

    @Override
    public List<MembershipDto> getByFamilyId(Long familyId) {
        return repo.findAllByFamily_Id(familyId)
                .stream().map(MembershipServiceImpl::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public MembershipDto invite(Long familyId, MembershipCreateDto dto) {
        Family fam = familyRepo.findById(familyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Family not found"));
        FamilyMembership m = new FamilyMembership();
        m.setFamily(fam);
        m.setUserId(dto.userId());
        m.setRole(RoleEnum.valueOf(dto.role()));
        // joinedAt se setea por defecto en la entidad
        return toDto(repo.save(m));
    }

    @Override
    public MembershipDto updateRole(Long familyId, Long id, MembershipUpdateDto dto) {
        FamilyMembership m = repo.findById(id)
                .filter(x -> x.getFamily().getId().equals(familyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership not found"));
        m.setRole(RoleEnum.valueOf(dto.role()));
        return toDto(repo.save(m));
    }
}
