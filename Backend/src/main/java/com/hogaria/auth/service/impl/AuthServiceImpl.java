package com.hogaria.auth.service.impl;

import com.hogaria.auth.dto.AuthResponse;
import com.hogaria.auth.dto.LoginRequest;
import com.hogaria.auth.dto.UserProfileResponse;
import com.hogaria.auth.dto.UserRegisterRequest;
import com.hogaria.auth.model.User;
import com.hogaria.auth.repository.UserRepository;
import com.hogaria.auth.service.AuthService;
import com.hogaria.config.security.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final int ACCESS_TOKEN_HOURS = 2;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthenticationManager authManager;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           TokenService tokenService,
                           AuthenticationManager authManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.authManager = authManager;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.debug("login() called with LoginRequest: username='{}'", request.username());

        // 1. Autenticar credenciales
        Authentication authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        User user = (User) authentication.getPrincipal();

        // 2. Generar JWT de acceso
        String jwt;
        try {
            jwt = tokenService.generateAccessToken(user);
        } catch (Exception ex) {
            log.error("login() failed - error generating JWT for user '{}'", user.getUsername(), ex);
            throw new RuntimeException("Error generating access token", ex);
        }

        // 3. Calcular expires_in en segundos segun la zona horaria del usuario
        Instant nowUtc = Instant.now();
        ZoneId userZone = ZoneId.of(user.getTimezone());
        ZonedDateTime nowInUserZone = nowUtc.atZone(ZoneOffset.UTC).withZoneSameInstant(userZone);
        ZonedDateTime expiryInUserZone = nowInUserZone.plusHours(ACCESS_TOKEN_HOURS);
        long expiresIn = Duration.between(nowInUserZone, expiryInUserZone).getSeconds();

        log.debug("login() successful for user '{}', token expires in {} seconds",
                user.getUsername(), expiresIn);

        return new AuthResponse(jwt, (int) expiresIn);
    }

    @Override
    @Transactional
    public UserProfileResponse register(UserRegisterRequest request) {
        log.debug("register() called with UserRegisterRequest: username='{}', email='{}', timezone='{}'",
                request.username(), request.email(), request.timezone());

        // 1. Validar unicidad de username
        if (userRepository.findByUsername(request.username()).isPresent()) {
            log.warn("register() failed - username '{}' already exists", request.username());
            throw new IllegalArgumentException("Username already in use");
        }

        // 2. Crear y guardar la entidad User
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole("ROLE_MEMBER");
        user.setTimezone(request.timezone());

        user = userRepository.save(user);
        log.debug("register() successful - created user id={}", user.getId());

        // 3. Mapear a DTO de perfil
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getTimezone(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
