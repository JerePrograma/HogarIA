package com.hogaria.auth.dto;


/**
 * DTO para actualizaciones parciales del perfil (p.ej. timezone o email).
 */
public record UserUpdateRequest(
        String email,
        String timezone
) {
}