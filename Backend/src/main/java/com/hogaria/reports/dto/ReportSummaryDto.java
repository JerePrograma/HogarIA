package com.hogaria.reports.dto;


import java.util.List;

public record ReportSummaryDto(
        double totalExpenses,
        double totalIncome,
        List<CategoryAmountDto> byCategory
) {
}
