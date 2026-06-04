package com.hogaria.service.transactionimport;

import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.entity.ImportRowStatus;

public final class TransactionImportRowStatusMapper {

    private TransactionImportRowStatusMapper() {
    }

    public static ImportRowStatus toImportRowStatus(RowStatus status) {
        if (status == null) {
            return ImportRowStatus.WARNING;
        }

        return switch (status) {
            case READY -> ImportRowStatus.READY;
            case SKIPPED -> ImportRowStatus.SKIPPED;
            case ERROR -> ImportRowStatus.ERROR;
            case NEEDS_CATEGORY,
                 DUPLICATE,
                 DUPLICATE_EXACT,
                 POSSIBLE_INTERNAL_TRANSFER,
                 INTERNAL_TRANSFER_MATCHED,
                 POSSIBLE_CROSS_SOURCE_DUPLICATE,
                 REVIEW -> ImportRowStatus.WARNING;
        };
    }
}