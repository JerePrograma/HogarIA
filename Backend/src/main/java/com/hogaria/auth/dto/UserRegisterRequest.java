package com.hogaria.auth.dto;


public record UserRegisterRequest(
        String username,
        String email,
        String password,
        String timezone
) {
}