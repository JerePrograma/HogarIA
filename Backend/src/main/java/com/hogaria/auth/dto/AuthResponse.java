package com.hogaria.auth.dto;

public record AuthResponse(
        String accessToken,
        Integer expiresIn
) {
}