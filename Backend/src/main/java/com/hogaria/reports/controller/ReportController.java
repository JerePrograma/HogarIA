package com.hogaria.reports.controller;

import com.hogaria.reports.dto.ReportSummaryDto;
import com.hogaria.reports.dto.ForecastDto;
import com.hogaria.reports.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/reports")
@Tag(name = "Reports", description = "Reportes de resumen y proyeccion de gastos")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @Operation(summary = "Resumen de gastos", description = "Calcula totales e importes por categoria")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen generado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ReportSummaryDto.class)))
    })
    @GetMapping("/summary")
    public ReportSummaryDto summary(
            @Parameter(description = "ID de la familia", required = true, in = ParameterIn.QUERY)
            @RequestParam("family_id") Long familyId,
            @Parameter(description = "Fecha inicial (yyyy-MM-dd)", in = ParameterIn.QUERY)
            @RequestParam(value = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Fecha final (yyyy-MM-dd)", in = ParameterIn.QUERY)
            @RequestParam(value = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return service.summarize(familyId, startDate, endDate);
    }

    @Operation(summary = "Forecast de gastos", description = "Proyeccion de proximos gastos en base a historicos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proyeccion calculada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ForecastDto.class)))
    })
    @GetMapping("/forecast")
    public ForecastDto forecast(
            @Parameter(description = "ID de la familia", required = true, in = ParameterIn.QUERY)
            @RequestParam("family_id") Long familyId,
            @Parameter(description = "Horizonte en dias para proyeccion", required = true, in = ParameterIn.QUERY)
            @RequestParam("horizon_days") int horizonDays
    ) {
        return service.forecast(familyId, horizonDays);
    }
}
