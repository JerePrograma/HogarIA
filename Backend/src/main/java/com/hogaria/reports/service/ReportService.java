package com.hogaria.reports.service;

import com.hogaria.reports.dto.ForecastDto;
import com.hogaria.reports.dto.ReportSummaryDto;

import java.time.LocalDate;

public interface ReportService {
    ReportSummaryDto summarize(
            Long familyId,
            LocalDate startDate,
            LocalDate endDate
    );

    ForecastDto forecast(
            Long familyId,
            int horizonDays
    );
}
