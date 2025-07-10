package com.hogaria.expense.controller;

import com.hogaria.expense.dto.CategoryDto;
import com.hogaria.expense.service.CategoryService;
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
@RequestMapping("/v1/categories")
@Tag(name = "Categories", description = "Gestion de categorias de gastos por familia")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @Operation(summary = "Listar categorias", description = "Devuelve todas las categorias de una familia")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de categorias",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = CategoryDto.class))))
    })
    @GetMapping
    public List<CategoryDto> list(
            @Parameter(description = "ID de la familia", required = true, in = ParameterIn.QUERY)
            @RequestParam("family_id") Long familyId
    ) {
        return service.getByFamilyId(familyId);
    }

    @Operation(summary = "Crear categoria", description = "Añade una nueva categoria a la familia")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoria creada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CategoryDto.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos")
    })
    @PostMapping
    public ResponseEntity<CategoryDto> create(
            @Parameter(description = "Datos de la nueva categoria", required = true)
            @RequestBody CategoryDto dto
    ) {
        return ResponseEntity.status(201).body(service.create(dto));
    }

    @Operation(summary = "Actualizar categoria", description = "Modifica el nombre o descripcion de una categoria existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria actualizada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CategoryDto.class))),
            @ApiResponse(responseCode = "404", description = "Categoria no encontrada")
    })
    @PutMapping("/{id}")
    public CategoryDto update(
            @Parameter(description = "ID de la categoria", required = true, in = ParameterIn.PATH)
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos de la categoria", required = true)
            @RequestBody CategoryDto dto
    ) {
        return service.update(id, dto);
    }

    @Operation(summary = "Eliminar categoria", description = "Borra una categoria por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Categoria eliminada"),
            @ApiResponse(responseCode = "404", description = "Categoria no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID de la categoria", required = true, in = ParameterIn.PATH)
            @PathVariable Long id
    ) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
