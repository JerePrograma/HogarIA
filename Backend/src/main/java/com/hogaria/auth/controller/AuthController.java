package com.hogaria.auth.controller;

import com.hogaria.auth.dto.AuthResponse;
import com.hogaria.auth.dto.LoginRequest;
import com.hogaria.auth.dto.UserProfileResponse;
import com.hogaria.auth.dto.UserRegisterRequest;
import com.hogaria.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Authentication", description = "Endpoints para login y registro de usuarios")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Login de usuario",
            description = "Valida credenciales y devuelve un JWT para su uso en Authorization header"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticacion correcta",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Formato de peticion invalido"),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Parameter(
                    description = "Objeto con username y password",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "LoginExample",
                                    value = "{ \"username\": \"jdoe\", \"password\": \"Passw0rd!\" }"
                            )
                    )
            )
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Registro de usuario",
            description = "Crea un nuevo usuario. Devuelve el perfil recien creado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario registrado con exito",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "409", description = "Username o email ya existe")
    })
    @PostMapping("/register")
    public ResponseEntity<UserProfileResponse> register(
            @Parameter(
                    description = "Datos para registrar nuevo usuario",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "RegisterExample",
                                    value = "{ \"username\": \"jdoe\", \"email\": \"jdoe@hogar.com\", \"password\": \"Passw0rd!\" }"
                            )
                    )
            )
            @Valid @RequestBody UserRegisterRequest request
    ) {
        UserProfileResponse profile = authService.register(request);
        return new ResponseEntity<>(profile, HttpStatus.CREATED);
    }
}
