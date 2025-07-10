package com.hogaria.house.dto;


import java.time.LocalDateTime;

public record MembershipDto(
        Long id,
        Long userId,
        Long familyId,
        String role,
        LocalDateTime joinedAt
) {
}
