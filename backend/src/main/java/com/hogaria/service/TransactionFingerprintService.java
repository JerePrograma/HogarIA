package com.hogaria.service;

import com.hogaria.entity.MoneyTransaction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TransactionFingerprintService {

    public String buildFingerprint(MoneyTransaction transaction) {
        return buildFingerprint(
                transaction.getProfileId(),
                transaction.getAccountId(),
                transaction.getOperationDateTime(),
                transaction.getRealDate() == null ? null : transaction.getRealDate().atStartOfDay(),
                transaction.getNormalizedDescription(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getSource(),
                transaction.getSourceOperationId()
        );
    }

    public String buildFingerprint(
            UUID profileId,
            UUID accountId,
            LocalDateTime operationDateTime,
            LocalDateTime fallbackDateTime,
            String normalizedDescription,
            BigDecimal amount,
            String currency,
            String source,
            String sourceOperationId
    ) {
        if (profileId == null || accountId == null || amount == null) {
            return null;
        }

        var effectiveDateTime = operationDateTime != null ? operationDateTime : fallbackDateTime;
        if (effectiveDateTime == null) {
            return null;
        }

        var normalizedAmount = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
        var normalizedCurrency = currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
        var normalizedSource = source == null ? "" : source.trim().toUpperCase(Locale.ROOT);
        var normalizedSourceOperation = sourceOperationId == null ? "" : sourceOperationId.trim();

        return sha256Hex(
                profileId + "|"
                        + accountId + "|"
                        + effectiveDateTime + "|"
                        + (normalizedDescription == null ? "" : normalizedDescription) + "|"
                        + normalizedAmount + "|"
                        + normalizedCurrency + "|"
                        + normalizedSource + "|"
                        + normalizedSourceOperation
        );
    }

    public String strongSourceKey(String profileId, String source, String sourceOperationId) {
        return sha256Hex(
                first(profileId) + "|"
                        + first(source).toUpperCase(Locale.ROOT) + "|"
                        + first(sourceOperationId)
        );
    }

    private String first(String value) {
        return value == null ? "" : value.trim();
    }

    private String sha256Hex(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            var builder = new StringBuilder(bytes.length * 2);

            for (var current : bytes) {
                builder.append(String.format("%02x", current));
            }

            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo calcular fingerprint SHA-256", ex);
        }
    }
}
