package com.hogaria.house.controller;

import com.hogaria.house.dto.MembershipCreateDto;
import com.hogaria.house.dto.MembershipDto;
import com.hogaria.house.dto.MembershipUpdateDto;
import com.hogaria.house.service.MembershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.web.bind.annotation.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.media.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/families/{familyId}/members")
@Tag(name = "Memberships", description = "Invitaciones y roles dentro de una familia")
public class MembershipController {

    private final MembershipService service;

    public MembershipController(MembershipService service) {
        this.service = service;
    }

    @Operation(summary = "Listar miembros", description = "Obtiene todas las membresias de una familia")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de miembros",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = MembershipDto.class))))
    })
    @GetMapping
    public List<MembershipDto> list(
            @Parameter(description = "ID de la familia", required = true, in = ParameterIn.PATH)
            @PathVariable Long familyId
    ) {
        return service.getByFamilyId(familyId);
    }

    @Operation(summary = "Invitar miembro", description = "Invita a un usuario y asigna un rol")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Invitacion enviada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MembershipDto.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos")
    })
    @PostMapping
    public ResponseEntity<MembershipDto> invite(
            @Parameter(description = "ID de la familia", required = true, in = ParameterIn.PATH)
            @PathVariable Long familyId,
            @RequestBody MembershipCreateDto dto
    ) {
        MembershipDto created = service.invite(familyId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Actualizar rol", description = "Modifica el rol de un miembro existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rol actualizado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MembershipDto.class))),
            @ApiResponse(responseCode = "404", description = "Membresia no encontrada")
    })
    @PutMapping("/{id}")
    public MembershipDto updateRole(
            @Parameter(description = "ID de la familia", required = true, in = ParameterIn.PATH)
            @PathVariable Long familyId,
            @Parameter(description = "ID de la membresia", required = true, in = ParameterIn.PATH)
            @PathVariable Long id,
            @RequestBody MembershipUpdateDto dto
    ) {
        return service.updateRole(familyId, id, dto);
    }
}
