package com.hogaria.inventory.controller;

import com.hogaria.inventory.dto.InventoryItemDto;
import com.hogaria.inventory.dto.CreateInventoryItemRequest;
import com.hogaria.inventory.dto.UpdateInventoryItemRequest;
import com.hogaria.inventory.dto.UnitDto;
import com.hogaria.inventory.service.InventoryService;
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
@RequestMapping("/v1/inventory-items")
@Tag(name = "Inventory", description = "Gestion de items de inventario")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @Operation(summary = "Listar items", description = "Devuelve items de inventario; opcionalmente solo los bajo threshold")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de items",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = InventoryItemDto.class))))
    })
    @GetMapping
    public List<InventoryItemDto> list(
            @Parameter(description = "ID de la familia", required = true, in = ParameterIn.QUERY)
            @RequestParam("family_id") Long familyId,
            @Parameter(description = "Si true, solo devuelve items con stock bajo threshold", in = ParameterIn.QUERY)
            @RequestParam(value = "threshold", required = false) Boolean threshold
    ) {
        return service.listItems(familyId, threshold);
    }

    @Operation(summary = "Crear item", description = "Añade un nuevo item al inventario")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "item creado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InventoryItemDto.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos")
    })
    @PostMapping
    public ResponseEntity<InventoryItemDto> create(
            @RequestBody CreateInventoryItemRequest dto
    ) {
        InventoryItemDto created = service.createItem(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Actualizar item", description = "Modifica cantidades u otras propiedades del item")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "item actualizado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InventoryItemDto.class))),
            @ApiResponse(responseCode = "404", description = "item no encontrado")
    })
    @PutMapping("/{id}")
    public InventoryItemDto update(
            @Parameter(description = "ID del item", required = true, in = ParameterIn.PATH)
            @PathVariable Long id,
            @RequestBody UpdateInventoryItemRequest dto
    ) {
        return service.updateItem(id, dto);
    }
}

