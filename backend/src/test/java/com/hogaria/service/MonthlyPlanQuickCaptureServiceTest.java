package com.hogaria.service;

import com.hogaria.dto.QuickCaptureDtos.QuickCaptureConfidence;
import com.hogaria.dto.QuickCaptureDtos.QuickCapturePreviewRequest;
import com.hogaria.entity.FinancialProfile;
import com.hogaria.entity.MonthlyPlanItem;
import com.hogaria.repository.FinancialProfileRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyPlanQuickCaptureServiceTest {
  @Mock FinancialProfileRepository profiles;
  MonthlyPlanQuickCaptureService service;
  UUID userId=UUID.randomUUID(); UUID profileId=UUID.randomUUID();

  @BeforeEach void setUp(){ service=new MonthlyPlanQuickCaptureService(profiles); when(profiles.findByIdAndUserId(profileId,userId)).thenReturn(Optional.of(new FinancialProfile())); }

  @Test void parseaFechaMontoCuota(){
    var r=service.preview(userId,profileId,new QuickCapturePreviewRequest("05/06 95000 Juliana cuota 3/5",2026,6,"ARS"));
    assertEquals(LocalDate.of(2026,6,5),r.parsed().expectedDate()); assertEquals(new BigDecimal("95000"),r.parsed().amount());
    assertEquals(3,r.parsed().installmentNumber()); assertEquals(5,r.parsed().installmentTotal());
  }

  @Test void noConfundeFechaConCuota(){
    var r=service.preview(userId,profileId,new QuickCapturePreviewRequest("05/06 95000 Juliana",2026,6,"ARS"));
    assertNull(r.parsed().installmentNumber()); assertNull(r.parsed().installmentTotal());
  }

  @Test void parseaRecuperoPorcentaje(){ var r=service.preview(userId,profileId,new QuickCapturePreviewRequest("hostel 550000 recupero 50% Agus",2026,6,"ARS"));
    assertEquals(MonthlyPlanItem.Type.EXPENSE,r.parsed().type()); assertEquals(new BigDecimal("550000"),r.parsed().amount());
    assertEquals(new BigDecimal("50"),r.parsed().expectedRecoveryPercent()); assertEquals("Agus",r.parsed().counterparty()); }

  @Test void recuperoIndependienteEsRecovery(){ var r=service.preview(userId,profileId,new QuickCapturePreviewRequest("Agus devuelve 275000",2026,6,"ARS"));
    assertEquals(MonthlyPlanItem.Type.RECOVERY,r.parsed().type()); }

  @Test void parseaRango(){ var r=service.preview(userId,profileId,new QuickCapturePreviewRequest("escuela Megu 180000 a 150000",2026,6,"ARS"));
    assertEquals(new BigDecimal("150000"),r.parsed().minAmount()); assertEquals(new BigDecimal("180000"),r.parsed().maxAmount()); }

  @Test void noInterpretaCtaComoMonto(){ var r=service.preview(userId,profileId,new QuickCapturePreviewRequest("cta 3/5",2026,6,"ARS"));
    assertNull(r.parsed().amount()); assertNull(r.parsed().minAmount()); assertNull(r.parsed().maxAmount()); }


  @Test void parseaRecuperoPorcentajeConContrapartePosterior(){ var r=service.preview(userId,profileId,new QuickCapturePreviewRequest("hostel 550000 recupero 50% Agus",2026,6,"ARS"));
    assertEquals(MonthlyPlanItem.Type.EXPENSE,r.parsed().type()); assertEquals(new BigDecimal("50"),r.parsed().expectedRecoveryPercent()); assertEquals("Agus",r.parsed().counterparty()); }

  @Test void parseaRecuperoPorcentajeConContraparteAnterior(){ var r=service.preview(userId,profileId,new QuickCapturePreviewRequest("Agus 50% hostel 550000",2026,6,"ARS"));
    assertEquals(MonthlyPlanItem.Type.EXPENSE,r.parsed().type()); assertEquals(new BigDecimal("50"),r.parsed().expectedRecoveryPercent()); assertEquals("Agus",r.parsed().counterparty()); }

  @Test void parseaRangoConPuntos(){ var r=service.preview(userId,profileId,new QuickCapturePreviewRequest("escuela Megu 150.000 a 180.000",2026,6,"ARS"));
    assertEquals(new BigDecimal("150000"),r.parsed().minAmount()); assertEquals(new BigDecimal("180000"),r.parsed().maxAmount()); assertNull(r.parsed().amount()); }

  @Test void parseaRangoConPesos(){ var r=service.preview(userId,profileId,new QuickCapturePreviewRequest("escuela Megu $150.000 - $180.000",2026,6,"ARS"));
    assertEquals(new BigDecimal("150000"),r.parsed().minAmount()); assertEquals(new BigDecimal("180000"),r.parsed().maxAmount()); assertNull(r.parsed().amount()); }

  @Test void noConfundeFechaConRango(){ var r=service.preview(userId,profileId,new QuickCapturePreviewRequest("05/06 95000 Juliana",2026,6,"ARS"));
    assertNull(r.parsed().minAmount()); assertNull(r.parsed().maxAmount()); assertEquals(new BigDecimal("95000"),r.parsed().amount()); }

  @Test void noConfundeCuotaConRango(){ var r=service.preview(userId,profileId,new QuickCapturePreviewRequest("cuota 3/5 95000 Juliana",2026,6,"ARS"));
    assertNull(r.parsed().minAmount()); assertNull(r.parsed().maxAmount()); assertEquals(new BigDecimal("95000"),r.parsed().amount()); }
  @Test void parseaTodoSinMonto(){ var r=service.preview(userId,profileId,new QuickCapturePreviewRequest("inflables Megu reservar fecha",2026,6,"ARS"));
    assertEquals(MonthlyPlanItem.Type.TODO,r.parsed().type()); assertNull(r.parsed().amount()); assertEquals(MonthlyPlanItem.Status.DRAFT,r.parsed().status()); }

  @Test void parseaIngreso(){ var r=service.preview(userId,profileId,new QuickCapturePreviewRequest("18/06 sueldo programación 1450000",2026,6,"ARS"));
    assertEquals(MonthlyPlanItem.Type.INCOME,r.parsed().type()); assertEquals(new BigDecimal("1450000"),r.parsed().amount()); assertEquals(LocalDate.of(2026,6,18),r.parsed().expectedDate()); }

  @Test void fechaInvalidaDevuelveWarningYLow(){ var r=service.preview(userId,profileId,new QuickCapturePreviewRequest("32/06 10000",2026,6,"ARS"));
    assertTrue(r.warnings().stream().anyMatch(x->x.contains("Fecha inválida"))); assertNull(r.parsed().expectedDate()); assertEquals(QuickCaptureConfidence.LOW,r.confidence()); }

  @Test void detectaCta(){ var r=service.preview(userId,profileId,new QuickCapturePreviewRequest("cta 4/5",2026,6,"ARS")); assertEquals(4,r.parsed().installmentNumber()); assertEquals(5,r.parsed().installmentTotal()); assertEquals(QuickCaptureConfidence.LOW,r.confidence()); }

  @Test void confirmarNoDaHigh(){ var r=service.preview(userId,profileId,new QuickCapturePreviewRequest("01/06 sueldo policía a confirmar",2026,6,"ARS")); assertNull(r.parsed().amount()); assertNotEquals(QuickCaptureConfidence.HIGH,r.confidence()); }

  @Test void perfiladoSinMontoQuedaDraft(){ var r=service.preview(userId,profileId,new QuickCapturePreviewRequest("perfilado cejas sin monto",2026,6,"ARS"));
    assertNull(r.parsed().amount()); assertEquals(MonthlyPlanItem.Status.DRAFT,r.parsed().status()); assertTrue(r.parsed().type()==MonthlyPlanItem.Type.EXPENSE || r.parsed().type()==MonthlyPlanItem.Type.TODO); }

  @Test void pasajesMiramarParseaBien(){ var r=service.preview(userId,profileId,new QuickCapturePreviewRequest("pasajes Miramar 3 findes 300000",2026,6,"ARS"));
    assertEquals(MonthlyPlanItem.Type.EXPENSE,r.parsed().type()); assertEquals(new BigDecimal("300000"),r.parsed().amount()); assertTrue(r.parsed().title().toLowerCase().contains("pasajes")); }
}
