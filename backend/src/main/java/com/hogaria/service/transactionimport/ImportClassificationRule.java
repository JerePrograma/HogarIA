package com.hogaria.service.transactionimport;

import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import java.util.function.Function;
import java.util.function.Predicate;

public record ImportClassificationRule(
        TransactionImportSource source,
        ClassificationLayer layer,
        int priority,
        String reasonCode,
        String fieldName,
        Predicate<NormalizedImportMovement> predicate,
        Function<NormalizedImportMovement, ImportClassificationResult> result
) {
  public boolean matches(NormalizedImportMovement movement) {
    return movement != null
            && movement.source() == source
            && predicate.test(movement);
  }
}
