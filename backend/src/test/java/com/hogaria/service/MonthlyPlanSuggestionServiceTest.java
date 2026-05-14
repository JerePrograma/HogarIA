package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.hogaria.dto.PlanningSuggestionDtos.PlanningSuggestionRequest;
import com.hogaria.entity.*;
import com.hogaria.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MonthlyPlanSuggestionServiceTest {
  @Mock MoneyTransactionRepository txRepo; @Mock MonthlyPlanItemRepository itemRepo; @Mock AccountRepository accountRepo; @Mock CategoryRepository categoryRepo; @Mock FinancialProfileRepository profileRepo;
  MonthlyPlanSuggestionService service; UUID userId=UUID.randomUUID(); UUID profileId=UUID.randomUUID(); UUID accountId=UUID.randomUUID(); UUID categoryId=UUID.randomUUID();
  @BeforeEach void setUp(){service=new MonthlyPlanSuggestionService(txRepo,itemRepo,accountRepo,categoryRepo,profileRepo); when(profileRepo.findByIdAndUserId(profileId,userId)).thenReturn(Optional.of(new FinancialProfile())); when(itemRepo.findByProfileId(profileId)).thenReturn(new ArrayList<>());}

  @Test void sugierePorTituloDesdeTransacciones(){ when(accountRepo.findByIdAndProfileId(accountId,profileId)).thenReturn(Optional.of(Account.builder().id(accountId).profileId(profileId).name("Banco").build())); when(categoryRepo.findById(categoryId)).thenReturn(Optional.of(Category.builder().id(categoryId).profileId(profileId).name("Servicios").build())); var tx=MoneyTransaction.builder().profileId(profileId).accountId(accountId).categoryId(categoryId).description("Hostel") .movementType(MoneyTransaction.MovementType.EXPENSE).amount(new BigDecimal("100000")).status(MoneyTransaction.Status.CONFIRMED).budgetDate(LocalDate.now()).updatedAt(LocalDateTime.now()).build(); when(txRepo.findByProfileId(profileId)).thenReturn(List.of(tx)); var r=service.suggest(userId,profileId,new PlanningSuggestionRequest(MonthlyPlanItem.Type.EXPENSE,"Pago Hostel",null,new BigDecimal("105000"),null,null,null,null)); assertNotNull(r.accountSuggestion()); assertNotNull(r.categorySuggestion()); }
  @Test void sugierePorContraparteDesdePlan(){ when(accountRepo.findByIdAndProfileId(accountId,profileId)).thenReturn(Optional.of(Account.builder().id(accountId).profileId(profileId).name("Banco").build())); when(categoryRepo.findById(categoryId)).thenReturn(Optional.of(Category.builder().id(categoryId).profileId(profileId).name("Servicios").build())); var i=MonthlyPlanItem.builder().profileId(profileId).accountId(accountId).categoryId(categoryId).title("Préstamo") .counterparty("Agus").type(MonthlyPlanItem.Type.DEBT).status(MonthlyPlanItem.Status.PAID).updatedAt(LocalDateTime.now()).build(); when(txRepo.findByProfileId(profileId)).thenReturn(List.of()); when(itemRepo.findByProfileId(profileId)).thenReturn(List.of(i)); var r=service.suggest(userId,profileId,new PlanningSuggestionRequest(MonthlyPlanItem.Type.DEBT,"Cuota", "Agus",new BigDecimal("10000"),null,null,null,null)); assertNotNull(r.accountSuggestion()); }
  @Test void ignoraCancelados(){ var i=MonthlyPlanItem.builder().profileId(profileId).accountId(accountId).categoryId(categoryId).title("Hostel").status(MonthlyPlanItem.Status.CANCELLED).build(); when(txRepo.findByProfileId(profileId)).thenReturn(List.of()); when(itemRepo.findByProfileId(profileId)).thenReturn(List.of(i)); var r=service.suggest(userId,profileId,new PlanningSuggestionRequest(MonthlyPlanItem.Type.EXPENSE,"Hostel",null,null,null,null,null,null)); assertEquals("Sin sugerencias confiables por ahora.",r.reasons().get(0)); }
  @Test void noneSinHistorial(){ when(txRepo.findByProfileId(profileId)).thenReturn(List.of()); var r=service.suggest(userId,profileId,new PlanningSuggestionRequest(MonthlyPlanItem.Type.EXPENSE,"X",null,null,null,null,null,null)); assertEquals("NONE",r.confidence().name()); }
}
