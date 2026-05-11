package com.hogaria.integration.cjprestamos.dto;

import java.util.List;

public record ExternalLoanManualSyncResponse(
    int loansSynced,
    int paymentsSynced,
    int movementsCreated,
    int skippedDuplicates,
    List<String> errors) {}
