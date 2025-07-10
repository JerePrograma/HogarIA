package com.hogaria.auth.dto;



import java.time.OffsetDateTime;

/**
 * Perfil de usuario completo.
 */
public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String timezone,
        String role,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}