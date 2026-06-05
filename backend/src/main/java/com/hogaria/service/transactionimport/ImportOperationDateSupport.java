package com.hogaria.service.transactionimport;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.regex.Pattern;

final class ImportOperationDateSupport {

    private static final Pattern BANCO_PROVINCIA_DEBIN_DATE = Pattern.compile(
            "\\bDB\\.DEBIN\\s+(\\d{1,2})/(\\d{1,2})\\b",
            Pattern.CASE_INSENSITIVE
    );

    private ImportOperationDateSupport() {
    }

    static LocalDate bancoProvinciaOperationDate(LocalDate bookedDate, String extendedDescription) {
        if (bookedDate == null || extendedDescription == null) {
            return bookedDate;
        }

        var matcher = BANCO_PROVINCIA_DEBIN_DATE.matcher(extendedDescription);
        if (!matcher.find()) {
            return bookedDate;
        }

        try {
            var operationDate = LocalDate.of(
                    bookedDate.getYear(),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(1))
            );

            if (operationDate.isAfter(bookedDate.plusMonths(6))) {
                return operationDate.minusYears(1);
            }
            if (operationDate.isBefore(bookedDate.minusMonths(6))) {
                return operationDate.plusYears(1);
            }
            return operationDate;
        } catch (DateTimeException | NumberFormatException ignored) {
            return bookedDate;
        }
    }
}
