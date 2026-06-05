package com.hogaria.service.transactionimport;

import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.entity.ImportRowStatus;

public final class TransactionImportRowStatusMapper {

    private TransactionImportRowStatusMapper() {
    }

    public static ImportRowStatus toImportRowStatus(RowStatus status) {
        if (status == null) {
            return ImportRowStatus.REVIEW;
        }

        return switch (status) {
            case READY -> ImportRowStatus.READY;
            case NEEDS_CATEGORY -> ImportRowStatus.NEEDS_CATEGORY;
            case REVIEW -> ImportRowStatus.REVIEW;
            case DUPLICATE -> ImportRowStatus.DUPLICATE;
            case DUPLICATE_EXACT -> ImportRowStatus.DUPLICATE_EXACT;
            case POSSIBLE_INTERNAL_TRANSFER -> ImportRowStatus.POSSIBLE_INTERNAL_TRANSFER;
            case INTERNAL_TRANSFER_MATCHED -> ImportRowStatus.INTERNAL_TRANSFER_MATCHED;
            case POSSIBLE_CROSS_SOURCE_DUPLICATE -> ImportRowStatus.POSSIBLE_CROSS_SOURCE_DUPLICATE;
            case SKIPPED -> ImportRowStatus.SKIPPED;
            case ERROR -> ImportRowStatus.ERROR;
        };
    }
}
