package com.hogaria.integration.cjprestamos.dto;

import java.util.List;
import java.util.Map;

public record ExternalLoanManualSyncResponse(
    boolean dryRun,
    int loansSynced,
    int paymentsSynced,
    int movementsCreated,
    int skippedDuplicates,
    List<String> errors,
    List<String> detectedLoans,
    List<String> detectedPayments,
    List<String> plannedMovements,
    Map<String, Integer> summaryByType) {}
