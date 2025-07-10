package com.hogaria.reports.dto;


import java.util.List;

public record ForecastDto(
        List<ForecastEntryDto> projectedExpenses,
        String advice
) {
}
