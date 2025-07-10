package com.hogaria.house.controller;

import com.hogaria.house.dto.CreateFamilyRequest;
import com.hogaria.house.dto.FamilyDto;
import com.hogaria.house.service.FamilyService;
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
@RequestMapping("/v1/houses/{houseId}/families")
@Tag(name = "Families", description = "Gestion de familias dentro de una casa")
public class FamilyController {

    private final FamilyService service;

    public FamilyController(FamilyService service) {
        this.service = service;
    }

    @Operation(summary = "Listar familias", description = "Devuelve todas las familias de una casa")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de familias",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = FamilyDto.class))))
    })
    @GetMapping
    public List<FamilyDto> list(
            @Parameter(description = "ID de la casa", required = true, in = ParameterIn.PATH)
            @PathVariable Long houseId
    ) {
        return service.getByHouseId(houseId);
    }

    @Operation(summary = "Crear familia", description = "Añade una nueva familia a la casa")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Familia creada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FamilyDto.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos")
    })
    @PostMapping
    public ResponseEntity<FamilyDto> create(
            @Parameter(description = "ID de la casa", required = true, in = ParameterIn.PATH)
            @PathVariable Long houseId,
            @RequestBody CreateFamilyRequest dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(houseId, dto));
    }
}
