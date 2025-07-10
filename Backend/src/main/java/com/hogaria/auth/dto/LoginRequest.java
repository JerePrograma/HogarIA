package com.hogaria.auth.dto;


public record LoginRequest(
        String username,
        String password
) {
}