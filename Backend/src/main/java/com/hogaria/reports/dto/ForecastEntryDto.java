package com.hogaria.reports.dto;

import java.time.LocalDate;

public record ForecastEntryDto(
        LocalDate date,
        double amount
) {
}
