package com.hogaria.auth.service;

import com.hogaria.auth.dto.AuthResponse;
import com.hogaria.auth.dto.LoginRequest;
import com.hogaria.auth.dto.UserProfileResponse;
import com.hogaria.auth.dto.UserRegisterRequest;

/**
 * Service interface for authentication operations.
 */
public interface AuthService {

    /**
     * Authenticates the user and retrieves a JWT token.
     *
     * @param request DTO containing username and password
     * @return AuthResponse containing token and expiry
     */
    AuthResponse login(LoginRequest request);

    /**
     * Registers a new user in the system.
     *
     * @param request DTO with registration details
     * @return UserProfileResponse with created user's info
     */
    UserProfileResponse register(UserRegisterRequest request);
}