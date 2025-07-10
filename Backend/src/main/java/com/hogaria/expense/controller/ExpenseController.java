package com.hogaria.expense.controller;

import com.hogaria.expense.dto.ExpenseDto;
import com.hogaria.expense.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.web.bind.annotation.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.media.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/expenses")
@Tag(name = "Expenses", description = "Gestion de gastos y filtros por fechas y categorias")
public class ExpenseController {

    private final ExpenseService service;

    public ExpenseController(ExpenseService service) {
        this.service = service;
    }

    @Operation(summary = "Listar gastos", description = "Obtiene los gastos filtrados por familia, fechas y categorias")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de gastos",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ExpenseDto.class))))
    })
    @GetMapping
    public List<ExpenseDto> listExpenses(
            @Parameter(description = "ID de la familia", required = true, in = ParameterIn.QUERY)
            @RequestParam("family_id") Long familyId,
            @Parameter(description = "Fecha inicial (yyyy-MM-dd)", in = ParameterIn.QUERY)
            @RequestParam(value = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Fecha final (yyyy-MM-dd)", in = ParameterIn.QUERY)
            @RequestParam(value = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "ID de categoria", in = ParameterIn.QUERY)
            @RequestParam(value = "category_id", required = false) Long categoryId,
            @Parameter(description = "ID de subcategoria", in = ParameterIn.QUERY)
            @RequestParam(value = "subcategory_id", required = false) Long subcategoryId
    ) {
        return service.getExpenses(familyId, startDate, endDate, categoryId, subcategoryId);
    }

    @Operation(summary = "Crear gasto", description = "Registra un nuevo gasto en la familia")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Gasto creado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ExpenseDto.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos")
    })
    @PostMapping
    public ResponseEntity<ExpenseDto> createExpense(
            @RequestBody ExpenseDto dto
    ) {
        ExpenseDto created = service.createExpense(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Actualizar gasto", description = "Modifica un gasto existente por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gasto actualizado"),
            @ApiResponse(responseCode = "404", description = "Gasto no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateExpense(
            @Parameter(description = "ID del gasto", required = true, in = ParameterIn.PATH)
            @PathVariable Long id,
            @RequestBody ExpenseDto dto
    ) {
        service.updateExpense(id, dto);
        return ResponseEntity.ok().build();
    }
}
