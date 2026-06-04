package com.hogaria.service.transactionimport;

import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ImportSourceHashService {

  public String fromOperationId(
          UUID profileId,
          UUID accountId,
          TransactionImportSource source,
          String sourceOperationId
  ) {
    return sha256(profileId + "|" + accountId + "|" + source.name() + "|" + sourceOperationId);
  }

  public String fromFallback(
          UUID profileId,
          UUID accountId,
          TransactionImportSource source,
          String fallback
  ) {
    return sha256(profileId + "|" + accountId + "|" + source.name() + "|" + fallback);
  }

  private String sha256(String value) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      var bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      var builder = new StringBuilder();

      for (byte current : bytes) {
        builder.append(String.format("%02x", current));
      }

      return builder.toString();
    } catch (Exception ex) {
      throw new IllegalStateException("No se pudo construir source_hash", ex);
    }
  }
}
