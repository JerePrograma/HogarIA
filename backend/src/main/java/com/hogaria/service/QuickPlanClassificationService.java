package com.hogaria.service;

import com.hogaria.dto.QuickPlanTextDtos.ClassificationResult;
import com.hogaria.entity.MonthlyPlanItem;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class QuickPlanClassificationService {
  public ClassificationResult classify(String rawLine) {
    String l = rawLine.toLowerCase(Locale.ROOT);
    MonthlyPlanItem.Priority priority = l.matches(".*\\b(si o si|urgente|vencimiento)\\b.*") ? MonthlyPlanItem.Priority.ESSENTIAL : MonthlyPlanItem.Priority.IMPORTANT;
    if (l.matches(".*\\b(tarjeta|mercadopago|mercado pago|cancelar|cuota)\\b.*")) return new ClassificationResult(MonthlyPlanItem.Type.DEBT, priority, null);
    if (l.matches(".*\\b(psicóloga|psiquiatra|médico|farmacia|psicologa|medico)\\b.*")) return new ClassificationResult(MonthlyPlanItem.Type.EXPENSE, priority, "salud");
    if (l.matches(".*\\b(viaje|mdp|transporte|nafta|viajes)\\b.*")) return new ClassificationResult(MonthlyPlanItem.Type.EXPENSE, priority, "transporte");
    return new ClassificationResult(MonthlyPlanItem.Type.EXPENSE, priority, null);
  }
}
