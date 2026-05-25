package com.hogaria.service;

import com.hogaria.entity.ExternalSyncMapping;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.integration.cjprestamos.dto.ExternalLoanBackfillDtos.*;
import com.hogaria.repository.ExternalSyncMappingRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import java.util.*;
import java.util.regex.*;
import org.springframework.stereotype.Service;

@Service
public class ExternalLoanBackfillService {
  private static final String SYSTEM = "CJPRESTAMOS";
  private final MoneyTransactionRepository transactionRepository;
  private final ExternalSyncMappingRepository mappingRepository;
  private final FinancialProfileRepository profileRepository;

  public ExternalLoanBackfillService(MoneyTransactionRepository t, ExternalSyncMappingRepository m, FinancialProfileRepository p) {
    this.transactionRepository = t; this.mappingRepository = m; this.profileRepository = p;
  }

  public BackfillDryRunResponse dryRun(UUID userId, UUID profileId) {
    ensure(profileId, userId);
    List<BackfillCandidate> out = new ArrayList<>();
    for (MoneyTransaction tx : transactionRepository.findByProfileId(profileId)) {
      if (!SYSTEM.equalsIgnoreCase(tx.getSource()) && (tx.getDescription()==null || !tx.getDescription().contains("CJ"))) continue;
      Inference inf = infer(tx);
      if (inf == null) continue;
      boolean exists = mappingRepository.findByProfileIdAndExternalSystemAndExternalEntityTypeAndExternalEntityIdAndExternalEventType(profileId,SYSTEM,inf.entityType,inf.entityId,inf.eventType).isPresent();
      out.add(new BackfillCandidate(tx.getId(), tx.getDescription(), tx.getAmount(), tx.getRealDate(), inf.entityType, inf.entityId, inf.eventType, inf.conf, exists?"mapping already exists":null, !exists));
    }
    return new BackfillDryRunResponse(out);
  }

  public BackfillApplyResponse apply(UUID userId, UUID profileId, BackfillApplyRequest request) {
    List<String> skipped = new ArrayList<>(), errors = new ArrayList<>(); int created=0;
    for (BackfillCandidate c : dryRun(userId, profileId).candidates()) {
      if (!c.wouldCreateMapping()) { skipped.add(c.transactionId()+": already mapped"); continue; }
      if ("LOW".equals(c.confidence()) && !request.includeLowConfidence()) { skipped.add(c.transactionId()+": low confidence"); continue; }
      try {
        ExternalSyncMapping m = ExternalSyncMapping.builder().profileId(profileId).externalSystem(SYSTEM)
            .externalEntityType(c.inferredEntityType()).externalEntityId(c.inferredEntityId())
            .externalEventType(c.inferredEventType()).status("PROCESSED").moneyTransactionId(c.transactionId()).build();
        mappingRepository.save(m); created++;
      } catch (Exception e) { skipped.add(c.transactionId()+": "+e.getMessage()); }
    }
    return new BackfillApplyResponse(created, skipped, errors);
  }

  private Inference infer(MoneyTransaction tx){
    if (tx.getSourceOperationId()!=null) {
      String[] p = tx.getSourceOperationId().split(":");
      if (p.length==3) return new Inference(p[0], p[1], p[2].equals("PRINCIPAL")?"PAYMENT_PRINCIPAL_RECOVERY":p[2].equals("INTEREST")?"PAYMENT_INTEREST_INCOME":p[2], "HIGH");
    }
    String d = Optional.ofNullable(tx.getDescription()).orElse("");
    Matcher m = Pattern.compile("Préstamo CJ #(\\d+)").matcher(d); if (m.find()) return new Inference("LOAN",m.group(1),"DISBURSEMENT","MEDIUM");
    m = Pattern.compile("Recupero capital CJ pago #(\\d+)").matcher(d); if (m.find()) return new Inference("PAYMENT",m.group(1),"PAYMENT_PRINCIPAL_RECOVERY","MEDIUM");
    m = Pattern.compile("Interés CJ pago #(\\d+)").matcher(d); if (m.find()) return new Inference("PAYMENT",m.group(1),"PAYMENT_INTEREST_INCOME","MEDIUM");
    return null;
  }
  private void ensure(UUID profileId, UUID userId){ if (!profileRepository.existsByIdAndUserId(profileId, userId)) throw new RuntimeException("forbidden"); }
  private record Inference(String entityType, String entityId, String eventType, String conf){}
}
