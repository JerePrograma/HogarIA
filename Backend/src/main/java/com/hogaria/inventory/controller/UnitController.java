package com.hogaria.inventory.controller;

import com.hogaria.inventory.dto.UnitDto;
import com.hogaria.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/units")
@Tag(name = "Units", description = "Listado de unidades disponibles en inventario")
public class UnitController {

    private final InventoryService service;

    public UnitController(InventoryService service) {
        this.service = service;
    }

    @Operation(summary = "Listar unidades", description = "Obtiene el catalogo de unidades de medida")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de unidades",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = UnitDto.class))))
    })
    @GetMapping
    public List<UnitDto> list() {
        return service.listUnits();
    }
}
