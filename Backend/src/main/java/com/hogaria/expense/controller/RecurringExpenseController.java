package com.hogaria.expense.controller;

import com.hogaria.expense.dto.RecurringExpenseDto;
import com.hogaria.expense.service.RecurringExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.web.bind.annotation.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.media.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/recurring-expenses")
@Tag(name = "RecurringExpenses", description = "Gastos recurrentes: CRUD completo")
public class RecurringExpenseController {

    private final RecurringExpenseService service;

    public RecurringExpenseController(RecurringExpenseService service) {
        this.service = service;
    }

    @Operation(summary = "Listar gastos recurrentes", description = "Obtiene los gastos recurrentes de una familia")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de recurrentes",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = RecurringExpenseDto.class))))
    })
    @GetMapping
    public List<RecurringExpenseDto> list(
            @Parameter(description = "ID de la familia", required = true, in = ParameterIn.QUERY)
            @RequestParam("family_id") Long familyId
    ) {
        return service.getByFamilyId(familyId);
    }

    @Operation(summary = "Crear gasto recurrente", description = "Añade un gasto recurrente a la familia")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recurrente creado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RecurringExpenseDto.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos")
    })
    @PostMapping
    public ResponseEntity<RecurringExpenseDto> create(
            @RequestBody RecurringExpenseDto dto
    ) {
        return ResponseEntity.status(201).body(service.create(dto));
    }

    @Operation(summary = "Actualizar recurrente", description = "Modifica un recurrente existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recurrente actualizado"),
            @ApiResponse(responseCode = "404", description = "No existe")
    })
    @PutMapping("/{id}")
    public RecurringExpenseDto update(
            @Parameter(description = "ID del recurrente", required = true, in = ParameterIn.PATH)
            @PathVariable Long id,
            @RequestBody RecurringExpenseDto dto
    ) {
        return service.update(id, dto);
    }

    @Operation(summary = "Eliminar recurrente", description = "Borra un recurrente por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Recurrente eliminado"),
            @ApiResponse(responseCode = "404", description = "No existe")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID del recurrente", required = true, in = ParameterIn.PATH)
            @PathVariable Long id
    ) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
