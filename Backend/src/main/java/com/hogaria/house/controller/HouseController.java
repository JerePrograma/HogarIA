package com.hogaria.house.controller;

import com.hogaria.house.dto.CreateHouseRequest;
import com.hogaria.house.dto.HouseDto;
import com.hogaria.house.service.HouseService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/v1/houses")
@Tag(name = "Houses", description = "Gestion de casas")
public class HouseController {

    private final HouseService service;

    public HouseController(HouseService service) {
        this.service = service;
    }

    @Operation(summary = "Listar casas", description = "Obtiene todas las casas registradas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de casas",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = HouseDto.class))))
    })
    @GetMapping
    public List<HouseDto> list() {
        return service.listAll();
    }

    @Operation(summary = "Crear casa", description = "Registra una nueva casa")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Casa creada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = HouseDto.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos")
    })
    @PostMapping
    public ResponseEntity<HouseDto> create(
            @RequestBody CreateHouseRequest dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(dto));
    }
}
