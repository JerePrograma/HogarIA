package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hogaria.dto.BudgetPlanningSuggestionDtos.ApplyBudgetSuggestion;
import com.hogaria.dto.BudgetPlanningSuggestionDtos.ApplyMonthlyPlanSuggestion;
import com.hogaria.dto.BudgetPlanningSuggestionDtos.BudgetPlanningSuggestionCommitRequest;
import com.hogaria.dto.BudgetPlanningSuggestionDtos.BudgetPlanningSuggestionPreviewRequest;
import com.hogaria.dto.BudgetPlanningSuggestionDtos.SuggestionMode;
import com.hogaria.dto.BudgetPlanningSuggestionDtos.SuggestionTarget;
import com.hogaria.entity.Account;
import com.hogaria.entity.BudgetCategoryItem;
import com.hogaria.entity.BudgetMonth;
import com.hogaria.entity.BudgetYear;
import com.hogaria.entity.Category;
import com.hogaria.entity.FinancialProfile;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.entity.MonthlyPlanItem;
import com.hogaria.repository.AccountRepository;
import com.hogaria.repository.BudgetCategoryItemRepository;
import com.hogaria.repository.BudgetMonthRepository;
import com.hogaria.repository.BudgetYearRepository;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import com.hogaria.repository.MonthlyPlanItemRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BudgetPlanningSuggestionServiceTest {

    @Mock FinancialProfileRepository profileRepository;
    @Mock MoneyTransactionRepository transactionRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock AccountRepository accountRepository;
    @Mock BudgetYearRepository budgetYearRepository;
    @Mock BudgetMonthRepository budgetMonthRepository;
    @Mock BudgetCategoryItemRepository budgetItemRepository;
    @Mock MonthlyPlanItemRepository monthlyPlanItemRepository;

    BudgetPlanningSuggestionService service;

    UUID userId = UUID.randomUUID();
    UUID profileId = UUID.randomUUID();
    UUID accountId = UUID.randomUUID();

    Map<UUID, Category> categories = new HashMap<>();

    @BeforeEach
    void setUp() {
        service = new BudgetPlanningSuggestionService(
                profileRepository,
                transactionRepository,
                categoryRepository,
                accountRepository,
                budgetYearRepository,
                budgetMonthRepository,
                budgetItemRepository,
                monthlyPlanItemRepository
        );

        lenient().when(profileRepository.findByIdAndUserId(profileId, userId))
                .thenReturn(Optional.of(FinancialProfile.builder().id(profileId).userId(userId).build()));

        lenient().when(categoryRepository.findAllById(Mockito.<Iterable<UUID>>any()))
                .thenAnswer(invocation -> {
                    Iterable<UUID> ids = invocation.getArgument(0);
                    List<Category> out = new ArrayList<>();

                    for (UUID id : ids) {
                        if (categories.containsKey(id)) {
                            out.add(categories.get(id));
                        }
                    }

                    return out;
                });

        lenient().when(categoryRepository.findById(any()))
                .thenAnswer(invocation -> Optional.ofNullable(categories.get(invocation.getArgument(0))));

        lenient().when(accountRepository.findAllById(Mockito.<Iterable<UUID>>any()))
                .thenReturn(List.of(Account.builder().id(accountId).profileId(profileId).name("Banco").build()));
        lenient().when(accountRepository.existsByIdAndProfileId(accountId, profileId)).thenReturn(true);

        lenient().when(budgetYearRepository.save(any())).thenAnswer(invocation -> {
            BudgetYear year = invocation.getArgument(0);
            if (year.getId() == null) {
                year.setId(UUID.randomUUID());
            }
            return year;
        });
        lenient().when(budgetMonthRepository.save(any())).thenAnswer(invocation -> {
            BudgetMonth month = invocation.getArgument(0);
            if (month.getId() == null) {
                month.setId(UUID.randomUUID());
            }
            return month;
        });
        lenient().when(budgetItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(monthlyPlanItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void generaPresupuestoDesdeGastosConfirmadosDeConsumo() {
        UUID categoryId = addCategory("Supermercado", Category.Type.VARIABLE_EXPENSE);
        stubTransactions(List.of(tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "1200", "Coto")));

        var response = service.preview(userId, profileId, preview(SuggestionMode.CURRENT_MONTH_ONLY, SuggestionTarget.BUDGET));

        assertEquals(1, response.budgetSuggestions().size());
        assertEquals(new BigDecimal("1200.00"), response.budgetSuggestions().get(0).suggestedBudgetAmount());
        assertEquals(new BigDecimal("1200.00"), response.budgetSuggestions().get(0).realAmount());
    }

    @Test
    void excluyeTransferenciasInternas() {
        UUID categoryId = addCategory("Transferencias", Category.Type.VARIABLE_EXPENSE);
        stubTransactions(List.of(tx(categoryId, MoneyTransaction.MovementType.TRANSFER, 2026, 5, "5000", "Fondeo interno")));

        var response = service.preview(userId, profileId, preview(SuggestionMode.CURRENT_MONTH_ONLY, SuggestionTarget.BUDGET));

        assertTrue(response.budgetSuggestions().isEmpty());
    }

    @Test
    void excluyePosiblesDuplicadosCrossSource() {
        UUID categoryId = addCategory("Compras", Category.Type.VARIABLE_EXPENSE);
        MoneyTransaction duplicate = tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "5000", "Compra tarjeta");
        duplicate.setClassificationReason("POSSIBLE_CROSS_SOURCE_DUPLICATE");
        stubTransactions(List.of(duplicate));

        var response = service.preview(userId, profileId, preview(SuggestionMode.CURRENT_MONTH_ONLY, SuggestionTarget.BUDGET));

        assertTrue(response.budgetSuggestions().isEmpty());
    }

    @Test
    void excluyeCjPrestamosDisbursementYRecoverableOutflow() {
        UUID categoryId = addCategory("CJ Capital prestado", Category.Type.INVESTMENT);
        MoneyTransaction disbursement = tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "90000", "Préstamo CJ");
        disbursement.setSource("CJPRESTAMOS");
        disbursement.setClassificationReason("CJPRESTAMOS_DISBURSEMENT");
        stubTransactions(List.of(disbursement));

        var response = service.preview(userId, profileId, preview(SuggestionMode.CURRENT_MONTH_ONLY, SuggestionTarget.BUDGET));

        assertTrue(response.budgetSuggestions().isEmpty());
    }

    @Test
    void excluyeCategoriasNoBudgetablesOTecnicas() {
        UUID goodId = addCategory("Alquiler", Category.Type.FIXED_EXPENSE);
        UUID technicalId = addCategory("Técnica", Category.Type.VARIABLE_EXPENSE);
        categories.get(technicalId).setTechnical(true);
        UUID notBudgetableId = addCategory("No budgetable", Category.Type.VARIABLE_EXPENSE);
        categories.get(notBudgetableId).setBudgetable(false);

        stubTransactions(List.of(
                tx(goodId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "1000", "Alquiler"),
                tx(technicalId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "2000", "Técnico"),
                tx(notBudgetableId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "3000", "No presupuesto")
        ));

        var response = service.preview(userId, profileId, preview(SuggestionMode.CURRENT_MONTH_ONLY, SuggestionTarget.BUDGET));

        assertEquals(1, response.budgetSuggestions().size());
        assertEquals(goodId, response.budgetSuggestions().get(0).categoryId());
    }

    @Test
    void incluyeCategoriasGlobalesActivasYPresupuestables() {
        UUID globalId = addGlobalCategory("Salud global", Category.Type.VARIABLE_EXPENSE);
        stubTransactions(List.of(tx(globalId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "25000", "Farmacia")));

        var response = service.preview(userId, profileId, preview(SuggestionMode.CURRENT_MONTH_ONLY, SuggestionTarget.BUDGET));

        assertEquals(1, response.budgetSuggestions().size());
        assertEquals(globalId, response.budgetSuggestions().get(0).categoryId());
    }

    @Test
    void includeImportedOnlyIgnoraManualesAunqueIncludeManualSeaTrue() {
        UUID categoryId = addCategory("Supermercado", Category.Type.VARIABLE_EXPENSE);
        MoneyTransaction imported = tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "1000", "Coto");
        MoneyTransaction manual = tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "5000", "Manual");
        manual.setOrigin(MoneyTransaction.Origin.MANUAL);
        manual.setSource(null);
        stubTransactions(List.of(imported, manual));

        var response = service.preview(userId, profileId, preview(
                SuggestionMode.CURRENT_MONTH_ONLY,
                SuggestionTarget.BUDGET,
                true,
                true,
                false,
                true,
                null
        ));

        assertEquals(new BigDecimal("1000.00"), response.budgetSuggestions().get(0).realAmount());
        assertEquals(1, response.budgetSuggestions().get(0).transactionCount());
    }

    @Test
    void includeImportedOnlyFalseConIncludeManualFalseSignificaSoloImportados() {
        UUID categoryId = addCategory("Supermercado", Category.Type.VARIABLE_EXPENSE);
        MoneyTransaction imported = tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "1000", "Coto");
        MoneyTransaction manual = tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "5000", "Manual");
        manual.setOrigin(MoneyTransaction.Origin.MANUAL);
        manual.setSource(null);
        stubTransactions(List.of(imported, manual));

        var response = service.preview(userId, profileId, preview(
                SuggestionMode.CURRENT_MONTH_ONLY,
                SuggestionTarget.BUDGET,
                false,
                false,
                false,
                true,
                null
        ));

        assertEquals(new BigDecimal("1000.00"), response.budgetSuggestions().get(0).realAmount());
        assertEquals(1, response.budgetSuggestions().get(0).transactionCount());
    }

    @Test
    void includeReviewIncluyeMovimientosEnRevision() {
        UUID categoryId = addCategory("Servicios", Category.Type.FIXED_EXPENSE);
        MoneyTransaction review = tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "15000", "Internet");
        review.setClassificationStatus(MoneyTransaction.ClassificationStatus.REVIEW);
        stubTransactions(List.of(review));

        var response = service.preview(userId, profileId, preview(
                SuggestionMode.CURRENT_MONTH_ONLY,
                SuggestionTarget.BUDGET,
                true,
                false,
                true,
                true,
                null
        ));

        assertEquals(1, response.budgetSuggestions().size());
    }

    @Test
    void selectedTransactionIdsIncluyeReviewSeleccionadoYExcluyeIdsInexistentes() {
        UUID categoryId = addCategory("Servicios", Category.Type.FIXED_EXPENSE);
        MoneyTransaction review = tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "15000", "Internet");
        review.setClassificationStatus(MoneyTransaction.ClassificationStatus.REVIEW);
        stubTransactions(List.of(review));

        var response = service.preview(userId, profileId, preview(
                SuggestionMode.CURRENT_MONTH_ONLY,
                SuggestionTarget.BUDGET,
                true,
                false,
                false,
                true,
                List.of(review.getId(), UUID.randomUUID())
        ));

        assertEquals(1, response.budgetSuggestions().size());
        assertEquals(List.of(review.getId()), response.budgetSuggestions().get(0).sourceTransactionIds());
    }

    @Test
    void selectedTransactionIdsNoSalteaReglasDuras() {
        UUID categoryId = addCategory("Transferencias", Category.Type.VARIABLE_EXPENSE);
        MoneyTransaction transfer = tx(categoryId, MoneyTransaction.MovementType.TRANSFER, 2026, 5, "15000", "Fondeo");
        MoneyTransaction pending = tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "12000", "Pendiente");
        pending.setStatus(MoneyTransaction.Status.PENDING);
        MoneyTransaction noCategory = tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "10000", "Sin categoría");
        noCategory.setCategoryId(null);
        stubTransactions(List.of(transfer, pending, noCategory));

        var response = service.preview(userId, profileId, preview(
                SuggestionMode.CURRENT_MONTH_ONLY,
                SuggestionTarget.BUDGET,
                true,
                false,
                false,
                true,
                List.of(transfer.getId(), pending.getId(), noCategory.getId())
        ));

        assertTrue(response.budgetSuggestions().isEmpty());
    }

    @Test
    void promedioUltimosTresMesesCalculaBien() {
        UUID categoryId = addCategory("Servicios", Category.Type.FIXED_EXPENSE);
        stubTransactions(List.of(
                tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 3, "1000", "Internet"),
                tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 4, "2000", "Internet"),
                tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "3000", "Internet")
        ));

        var response = service.preview(userId, profileId, preview(SuggestionMode.LAST_3_MONTHS_AVERAGE, SuggestionTarget.BUDGET));

        assertEquals(new BigDecimal("2000.00"), response.budgetSuggestions().get(0).suggestedBudgetAmount());
    }

    @Test
    void outlierDeUnGastoGrandeQuedaMarcadoYSinAplicacionAutomatica() {
        UUID categoryId = addCategory("Viajes", Category.Type.VARIABLE_EXPENSE);
        stubTransactions(List.of(
                tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 3, "10000", "Viaje"),
                tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 4, "10000", "Viaje"),
                tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "100000", "Viaje")
        ));

        var response = service.preview(userId, profileId, preview(SuggestionMode.LAST_3_MONTHS_AVERAGE, SuggestionTarget.BUDGET));

        assertTrue(response.budgetSuggestions().get(0).outlierDetected());
        assertTrue(response.budgetSuggestions().get(0).outlierAffectsSuggestedAmount());
        assertFalse(response.budgetSuggestions().get(0).applyByDefault());
        assertEquals(new BigDecimal("10000.00"), response.budgetSuggestions().get(0).suggestedBudgetAmount());
    }

    @Test
    void outlierDeMesAnteriorNoContaminaPromedioTresMeses() {
        UUID categoryId = addCategory("Viajes", Category.Type.VARIABLE_EXPENSE);
        stubTransactions(List.of(
                tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 3, "100000", "Viaje excepcional"),
                tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 4, "10000", "Transporte"),
                tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "10000", "Transporte")
        ));

        var response = service.preview(userId, profileId, preview(SuggestionMode.LAST_3_MONTHS_AVERAGE, SuggestionTarget.BUDGET));

        assertTrue(response.budgetSuggestions().get(0).outlierDetected());
        assertTrue(response.budgetSuggestions().get(0).outlierAffectsSuggestedAmount());
        assertFalse(response.budgetSuggestions().get(0).applyByDefault());
        assertEquals(new BigDecimal("10000.00"), response.budgetSuggestions().get(0).suggestedBudgetAmount());
    }

    @Test
    void outlierEnVentanaSeisMesesConCuatroMesesDeDatosSeExcluyeDelPromedio() {
        UUID categoryId = addCategory("Salud", Category.Type.VARIABLE_EXPENSE);
        stubTransactions(List.of(
                tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 2, "10000", "Farmacia"),
                tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 3, "10000", "Farmacia"),
                tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 4, "100000", "Cirugía excepcional"),
                tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "10000", "Farmacia")
        ));

        var response = service.preview(userId, profileId, preview(SuggestionMode.LAST_6_MONTHS_AVERAGE, SuggestionTarget.BUDGET));

        assertTrue(response.budgetSuggestions().get(0).outlierDetected());
        assertTrue(response.budgetSuggestions().get(0).outlierAffectsSuggestedAmount());
        assertEquals(new BigDecimal("10000.00"), response.budgetSuggestions().get(0).suggestedBudgetAmount());
    }

    @Test
    void noMarcaComoOutlierUnPatronRecurrenteAlto() {
        UUID categoryId = addCategory("Colegio", Category.Type.FIXED_EXPENSE);
        stubTransactions(List.of(
                tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 2, "100000", "Colegio"),
                tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 3, "105000", "Colegio"),
                tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 4, "98000", "Colegio"),
                tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "102000", "Colegio")
        ));

        var response = service.preview(userId, profileId, preview(SuggestionMode.LAST_6_MONTHS_AVERAGE, SuggestionTarget.BUDGET));

        assertFalse(response.budgetSuggestions().get(0).outlierDetected());
        assertTrue(response.budgetSuggestions().get(0).applyByDefault());
    }

    @Test
    void commitNoCreaEstructuraPresupuestariaSiTodasLasSugerenciasSonInvalidas() {
        UUID missingCategoryId = UUID.randomUUID();

        var response = service.commit(userId, profileId, commitBudget(missingCategoryId, "100000", false));

        assertEquals(0, response.createdBudgetItems());
        assertFalse(response.errors().isEmpty());
        verify(budgetYearRepository, never()).save(any(BudgetYear.class));
        verify(budgetMonthRepository, never()).save(any(BudgetMonth.class));
        verify(budgetItemRepository, never()).save(any(BudgetCategoryItem.class));
    }

    @Test
    void commitCreaBudgetYearYBudgetMonthSiNoExisten() {
        UUID categoryId = addCategory("Alquiler", Category.Type.FIXED_EXPENSE);
        when(budgetYearRepository.findByProfileIdAndYear(profileId, 2026)).thenReturn(Optional.empty());
        when(budgetMonthRepository.findByBudgetYearIdAndMonth(any(), eq(5))).thenReturn(Optional.empty());
        when(budgetItemRepository.findByBudgetMonthIdAndCategoryId(any(), eq(categoryId))).thenReturn(Optional.empty());

        var response = service.commit(userId, profileId, commitBudget(categoryId, "100000", false));

        assertEquals(1, response.createdBudgetItems());
        assertEquals(0, response.updatedBudgetItems());
        verify(budgetYearRepository).save(any(BudgetYear.class));
        verify(budgetMonthRepository).save(any(BudgetMonth.class));
    }

    @Test
    void commitNoPisaPresupuestoExistenteSinOverwrite() {
        UUID categoryId = addCategory("Alquiler", Category.Type.FIXED_EXPENSE);
        UUID budgetYearId = UUID.randomUUID();
        UUID budgetMonthId = UUID.randomUUID();
        BudgetCategoryItem existing = BudgetCategoryItem.builder()
                .budgetMonthId(budgetMonthId)
                .categoryId(categoryId)
                .budgetAmount(new BigDecimal("80000"))
                .build();
        when(budgetYearRepository.findByProfileIdAndYear(profileId, 2026))
                .thenReturn(Optional.of(BudgetYear.builder().id(budgetYearId).profileId(profileId).year(2026).build()));
        when(budgetMonthRepository.findByBudgetYearIdAndMonth(budgetYearId, 5))
                .thenReturn(Optional.of(BudgetMonth.builder().id(budgetMonthId).budgetYearId(budgetYearId).month(5).build()));
        when(budgetItemRepository.findByBudgetMonthIdAndCategoryId(budgetMonthId, categoryId))
                .thenReturn(Optional.of(existing));

        var response = service.commit(userId, profileId, commitBudget(categoryId, "100000", false));

        assertEquals(0, response.updatedBudgetItems());
        assertEquals(1, response.skippedDuplicates());
        assertEquals(new BigDecimal("80000"), existing.getBudgetAmount());
    }

    @Test
    void commitActualizaPresupuestoExistenteConOverwrite() {
        UUID categoryId = addCategory("Alquiler", Category.Type.FIXED_EXPENSE);
        UUID budgetYearId = UUID.randomUUID();
        UUID budgetMonthId = UUID.randomUUID();
        BudgetCategoryItem existing = BudgetCategoryItem.builder()
                .budgetMonthId(budgetMonthId)
                .categoryId(categoryId)
                .budgetAmount(new BigDecimal("80000"))
                .build();
        when(budgetYearRepository.findByProfileIdAndYear(profileId, 2026))
                .thenReturn(Optional.of(BudgetYear.builder().id(budgetYearId).profileId(profileId).year(2026).build()));
        when(budgetMonthRepository.findByBudgetYearIdAndMonth(budgetYearId, 5))
                .thenReturn(Optional.of(BudgetMonth.builder().id(budgetMonthId).budgetYearId(budgetYearId).month(5).build()));
        when(budgetItemRepository.findByBudgetMonthIdAndCategoryId(budgetMonthId, categoryId))
                .thenReturn(Optional.of(existing));

        var response = service.commit(userId, profileId, commitBudget(categoryId, "100000", true));

        assertEquals(1, response.updatedBudgetItems());
        assertEquals(new BigDecimal("100000.00"), existing.getBudgetAmount());
    }

    @Test
    void gastoRecurrenteGeneraMonthlyPlanItemSugeridoParaProximoMes() {
        UUID categoryId = addCategory("Alquiler", Category.Type.FIXED_EXPENSE);
        stubTransactions(List.of(
                tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 4, "100000", "Alquiler"),
                tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "100000", "Alquiler")
        ));
        when(monthlyPlanItemRepository.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, 2026, 6))
                .thenReturn(List.of());

        var response = service.preview(userId, profileId, preview(SuggestionMode.LAST_3_MONTHS_AVERAGE, SuggestionTarget.MONTHLY_PLAN));

        assertEquals(1, response.monthlyPlanSuggestions().size());
        assertEquals(MonthlyPlanItem.Type.EXPENSE, response.monthlyPlanSuggestions().get(0).type());
        assertEquals(2026, response.monthlyPlanSuggestions().get(0).periodYear());
        assertEquals(6, response.monthlyPlanSuggestions().get(0).periodMonth());
    }

    @Test
    void planificacionPuedeApuntarAlMesSeleccionadoSiNextMonthEsFalse() {
        UUID categoryId = addCategory("Alquiler", Category.Type.FIXED_EXPENSE);
        stubTransactions(List.of(tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 4, "100000", "Alquiler")));
        when(monthlyPlanItemRepository.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, 2026, 5))
                .thenReturn(List.of());

        var response = service.preview(userId, profileId, preview(
                SuggestionMode.LAST_3_MONTHS_AVERAGE,
                SuggestionTarget.MONTHLY_PLAN,
                true,
                false,
                false,
                false,
                null
        ));

        assertEquals(1, response.monthlyPlanSuggestions().size());
        assertEquals(2026, response.monthlyPlanSuggestions().get(0).periodYear());
        assertEquals(5, response.monthlyPlanSuggestions().get(0).periodMonth());
        assertEquals(LocalDate.of(2026, 5, 5), response.monthlyPlanSuggestions().get(0).expectedDate());
    }

    @Test
    void planificacionExcluyeCategoriasTecnicasEInactivas() {
        UUID goodId = addCategory("Alquiler", Category.Type.FIXED_EXPENSE);
        UUID technicalId = addCategory("Fondeo técnico", Category.Type.FIXED_EXPENSE);
        categories.get(technicalId).setTechnical(true);
        UUID inactiveId = addCategory("Inactiva", Category.Type.FIXED_EXPENSE);
        categories.get(inactiveId).setActive(false);
        stubTransactions(List.of(
                tx(goodId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "100000", "Alquiler"),
                tx(technicalId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "50000", "Fondeo"),
                tx(inactiveId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "60000", "Viejo")
        ));
        when(monthlyPlanItemRepository.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, 2026, 6))
                .thenReturn(List.of());

        var response = service.preview(userId, profileId, preview(SuggestionMode.CURRENT_MONTH_ONLY, SuggestionTarget.MONTHLY_PLAN));

        assertEquals(1, response.monthlyPlanSuggestions().size());
        assertEquals(goodId, response.monthlyPlanSuggestions().get(0).categoryId());
    }

    @Test
    void tarjetaCreditoGeneraTypeDebt() {
        UUID categoryId = addCategory("Tarjeta", Category.Type.DEBT);
        MoneyTransaction tx = tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "45000", "Pago VISA");
        tx.setPaymentChannel(MoneyTransaction.PaymentChannel.CREDIT_CARD);
        stubTransactions(List.of(tx));
        when(monthlyPlanItemRepository.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, 2026, 6))
                .thenReturn(List.of());

        var response = service.preview(userId, profileId, preview(SuggestionMode.CURRENT_MONTH_ONLY, SuggestionTarget.MONTHLY_PLAN));

        assertEquals(MonthlyPlanItem.Type.DEBT, response.monthlyPlanSuggestions().get(0).type());
    }

    @Test
    void ingresoRecurrenteGeneraTypeIncome() {
        UUID categoryId = addCategory("Sueldo", Category.Type.INCOME);
        stubTransactions(List.of(
                tx(categoryId, MoneyTransaction.MovementType.INCOME, 2026, 4, "900000", "Sueldo"),
                tx(categoryId, MoneyTransaction.MovementType.INCOME, 2026, 5, "900000", "Sueldo")
        ));
        when(monthlyPlanItemRepository.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, 2026, 6))
                .thenReturn(List.of());

        var response = service.preview(userId, profileId, preview(SuggestionMode.LAST_3_MONTHS_AVERAGE, SuggestionTarget.MONTHLY_PLAN));

        assertEquals(MonthlyPlanItem.Type.INCOME, response.monthlyPlanSuggestions().get(0).type());
    }

    @Test
    void transferenciaOFondeoNoGeneraPlanificacion() {
        UUID categoryId = addCategory("Fondeo", Category.Type.VARIABLE_EXPENSE);
        MoneyTransaction tx = tx(categoryId, MoneyTransaction.MovementType.TRANSFER, 2026, 5, "50000", "Fondeo MercadoPago");
        tx.setClassificationReason("INTERNAL_TRANSFER_MATCHED");
        stubTransactions(List.of(tx));

        var response = service.preview(userId, profileId, preview(SuggestionMode.CURRENT_MONTH_ONLY, SuggestionTarget.MONTHLY_PLAN));

        assertTrue(response.monthlyPlanSuggestions().isEmpty());
    }

    @Test
    void cjCapitalPrestadoNoGeneraGastoPlanificado() {
        UUID categoryId = addCategory("CJ Capital prestado", Category.Type.INVESTMENT);
        MoneyTransaction tx = tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "70000", "Préstamo CJ");
        tx.setSource("CJPRESTAMOS");
        tx.setClassificationReason("CJPRESTAMOS_DISBURSEMENT");
        stubTransactions(List.of(tx));

        var response = service.preview(userId, profileId, preview(SuggestionMode.CURRENT_MONTH_ONLY, SuggestionTarget.MONTHLY_PLAN));

        assertTrue(response.monthlyPlanSuggestions().isEmpty());
    }

    @Test
    void bancoProvinciaPrestamoImportadoGeneraDeudaEstimada() {
        UUID categoryId = addCategory("Créditos", Category.Type.DEBT);
        MoneyTransaction tx = tx(categoryId, MoneyTransaction.MovementType.EXPENSE, 2026, 5, "60000", "Préstamo Banco Provincia");
        tx.setSource("BANCO_PROVINCIA");
        stubTransactions(List.of(tx));
        when(monthlyPlanItemRepository.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, 2026, 6))
                .thenReturn(List.of());

        var response = service.preview(userId, profileId, preview(SuggestionMode.CURRENT_MONTH_ONLY, SuggestionTarget.MONTHLY_PLAN));

        assertEquals(MonthlyPlanItem.Type.DEBT, response.monthlyPlanSuggestions().get(0).type());
        assertEquals(MonthlyPlanItem.Source.IMPORT, response.monthlyPlanSuggestions().get(0).source());
    }

    @Test
    void commitPlanificacionDerivaPeriodoDesdeFechaEsperada() {
        UUID categoryId = addCategory("Alquiler", Category.Type.FIXED_EXPENSE);
        var request = commitPlan(new ApplyMonthlyPlanSuggestion(
                "Alquiler",
                null,
                LocalDate.of(2026, 7, 5),
                2026,
                6,
                new BigDecimal("100000.00"),
                null,
                null,
                categoryId,
                accountId,
                MonthlyPlanItem.Type.EXPENSE,
                MonthlyPlanItem.Priority.ESSENTIAL,
                MonthlyPlanItem.Source.SYSTEM,
                true,
                false,
                List.of()
        ));

        var response = service.commit(userId, profileId, request);

        assertEquals(1, response.createdMonthlyPlanItems());
        assertTrue(response.errors().isEmpty());
        var captor = ArgumentCaptor.forClass(MonthlyPlanItem.class);
        verify(monthlyPlanItemRepository).save(captor.capture());
        assertEquals(2026, captor.getValue().getPeriodYear());
        assertEquals(7, captor.getValue().getPeriodMonth());
    }

    @Test
    void dedupeEvitaCrearMonthlyPlanItemRepetido() {
        UUID categoryId = addCategory("Alquiler", Category.Type.FIXED_EXPENSE);
        MonthlyPlanItem existing = MonthlyPlanItem.builder()
                .profileId(profileId)
                .categoryId(categoryId)
                .title("Alquiler")
                .periodYear(2026)
                .periodMonth(6)
                .amount(new BigDecimal("100000.00"))
                .type(MonthlyPlanItem.Type.EXPENSE)
                .source(MonthlyPlanItem.Source.SYSTEM)
                .status(MonthlyPlanItem.Status.ESTIMATED)
                .build();
        when(monthlyPlanItemRepository.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, 2026, 6))
                .thenReturn(List.of(existing));

        var request = new BudgetPlanningSuggestionCommitRequest(
                2026,
                5,
                List.of(),
                List.of(new ApplyMonthlyPlanSuggestion(
                        "Alquiler",
                        null,
                        LocalDate.of(2026, 6, 5),
                        2026,
                        6,
                        new BigDecimal("100000.00"),
                        null,
                        null,
                        categoryId,
                        accountId,
                        MonthlyPlanItem.Type.EXPENSE,
                        MonthlyPlanItem.Priority.ESSENTIAL,
                        MonthlyPlanItem.Source.SYSTEM,
                        true,
                        false,
                        List.of()
                )),
                true,
                false
        );

        var response = service.commit(userId, profileId, request);

        assertEquals(0, response.createdMonthlyPlanItems());
        assertEquals(1, response.skippedDuplicates());
        verify(monthlyPlanItemRepository, never()).save(any());
    }

    @Test
    void dedupeDetectaMismoTituloCategoriaYMontoAunqueCambieSource() {
        UUID categoryId = addCategory("Alquiler", Category.Type.FIXED_EXPENSE);
        when(monthlyPlanItemRepository.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, 2026, 6))
                .thenReturn(List.of(existingPlan(categoryId, "Alquiler", "100000.00", MonthlyPlanItem.Source.IMPORT, MonthlyPlanItem.Status.ESTIMATED)));

        var response = service.commit(userId, profileId, commitPlan(planSuggestion(categoryId, "Alquiler", "100000.00", MonthlyPlanItem.Source.SYSTEM)));

        assertEquals(0, response.createdMonthlyPlanItems());
        assertEquals(1, response.skippedDuplicates());
    }

    @Test
    void dedupeDetectaTituloParecidoConTokensCompartidos() {
        UUID categoryId = addCategory("Alquiler", Category.Type.FIXED_EXPENSE);
        when(monthlyPlanItemRepository.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, 2026, 6))
                .thenReturn(List.of(existingPlan(categoryId, "Alquiler departamento", "100000.00", MonthlyPlanItem.Source.SYSTEM, MonthlyPlanItem.Status.ESTIMATED)));

        var response = service.commit(userId, profileId, commitPlan(planSuggestion(categoryId, "Alquiler vivienda", "100000.00", MonthlyPlanItem.Source.SYSTEM)));

        assertEquals(0, response.createdMonthlyPlanItems());
        assertEquals(1, response.skippedDuplicates());
    }

    @Test
    void dedupeNoBloqueaMismoTituloConCategoriaDistinta() {
        UUID categoryId = addCategory("Alquiler", Category.Type.FIXED_EXPENSE);
        UUID otherCategoryId = addCategory("Servicios", Category.Type.FIXED_EXPENSE);
        when(monthlyPlanItemRepository.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, 2026, 6))
                .thenReturn(List.of(existingPlan(otherCategoryId, "Alquiler", "100000.00", MonthlyPlanItem.Source.SYSTEM, MonthlyPlanItem.Status.ESTIMATED)));

        var response = service.commit(userId, profileId, commitPlan(planSuggestion(categoryId, "Alquiler", "100000.00", MonthlyPlanItem.Source.SYSTEM)));

        assertEquals(1, response.createdMonthlyPlanItems());
        assertEquals(0, response.skippedDuplicates());
    }

    @Test
    void dedupeDetectaMontoSimilarDentroDeTolerancia() {
        UUID categoryId = addCategory("Alquiler", Category.Type.FIXED_EXPENSE);
        when(monthlyPlanItemRepository.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, 2026, 6))
                .thenReturn(List.of(existingPlan(categoryId, "Alquiler", "100000.00", MonthlyPlanItem.Source.SYSTEM, MonthlyPlanItem.Status.ESTIMATED)));

        var response = service.commit(userId, profileId, commitPlan(planSuggestion(categoryId, "Alquiler", "109000.00", MonthlyPlanItem.Source.SYSTEM)));

        assertEquals(0, response.createdMonthlyPlanItems());
        assertEquals(1, response.skippedDuplicates());
    }

    @Test
    void dedupeNoBloqueaMontoFueraDeTolerancia() {
        UUID categoryId = addCategory("Alquiler", Category.Type.FIXED_EXPENSE);
        when(monthlyPlanItemRepository.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, 2026, 6))
                .thenReturn(List.of(existingPlan(categoryId, "Alquiler", "100000.00", MonthlyPlanItem.Source.SYSTEM, MonthlyPlanItem.Status.ESTIMATED)));

        var response = service.commit(userId, profileId, commitPlan(planSuggestion(categoryId, "Alquiler", "125000.00", MonthlyPlanItem.Source.SYSTEM)));

        assertEquals(1, response.createdMonthlyPlanItems());
        assertEquals(0, response.skippedDuplicates());
    }

    @Test
    void dedupeNoBloqueaItemCancelado() {
        UUID categoryId = addCategory("Alquiler", Category.Type.FIXED_EXPENSE);
        when(monthlyPlanItemRepository.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, 2026, 6))
                .thenReturn(List.of(existingPlan(categoryId, "Alquiler", "100000.00", MonthlyPlanItem.Source.SYSTEM, MonthlyPlanItem.Status.CANCELLED)));

        var response = service.commit(userId, profileId, commitPlan(planSuggestion(categoryId, "Alquiler", "100000.00", MonthlyPlanItem.Source.SYSTEM)));

        assertEquals(1, response.createdMonthlyPlanItems());
        assertEquals(0, response.skippedDuplicates());
    }

    private UUID addCategory(String name, Category.Type type) {
        UUID id = UUID.randomUUID();
        categories.put(id, Category.builder()
                .id(id)
                .profileId(profileId)
                .name(name)
                .type(type)
                .scope(Category.Scope.PERSONAL)
                .active(true)
                .budgetable(true)
                .technical(false)
                .build());
        return id;
    }

    private UUID addGlobalCategory(String name, Category.Type type) {
        UUID id = UUID.randomUUID();
        categories.put(id, Category.builder()
                .id(id)
                .profileId(null)
                .name(name)
                .type(type)
                .scope(Category.Scope.GLOBAL)
                .active(true)
                .budgetable(true)
                .technical(false)
                .build());
        return id;
    }

    private MoneyTransaction tx(
            UUID categoryId,
            MoneyTransaction.MovementType movementType,
            int year,
            int month,
            String amount,
            String description
    ) {
        return MoneyTransaction.builder()
                .id(UUID.randomUUID())
                .profileId(profileId)
                .accountId(accountId)
                .categoryId(categoryId)
                .movementType(movementType)
                .realDate(LocalDate.of(year, month, Math.min(5, LocalDate.of(year, month, 1).lengthOfMonth())))
                .budgetDate(LocalDate.of(year, month, 1))
                .amount(new BigDecimal(amount))
                .currency("ARS")
                .description(description)
                .origin(MoneyTransaction.Origin.IMPORT)
                .status(MoneyTransaction.Status.CONFIRMED)
                .classificationStatus(MoneyTransaction.ClassificationStatus.CLASSIFIED)
                .source("MERCADO_PAGO")
                .paymentChannel(MoneyTransaction.PaymentChannel.UNKNOWN)
                .build();
    }

    private void stubTransactions(List<MoneyTransaction> transactions) {
        when(transactionRepository.findByProfileIdAndBudgetDateBetween(eq(profileId), any(), any()))
                .thenAnswer(invocation -> {
                    LocalDate from = invocation.getArgument(1);
                    LocalDate to = invocation.getArgument(2);

                    return transactions.stream()
                            .filter(tx -> !tx.getBudgetDate().isBefore(from) && !tx.getBudgetDate().isAfter(to))
                            .toList();
                });

        when(transactionRepository.findByProfileIdAndIdIn(eq(profileId), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<UUID> ids = invocation.getArgument(1);
                    return transactions.stream()
                            .filter(tx -> ids.contains(tx.getId()))
                            .toList();
                });
    }

    private BudgetPlanningSuggestionPreviewRequest preview(
            SuggestionMode mode,
            SuggestionTarget target
    ) {
        return preview(mode, target, true, false, false, true, null);
    }

    private BudgetPlanningSuggestionPreviewRequest preview(
            SuggestionMode mode,
            SuggestionTarget target,
            boolean includeImportedOnly,
            boolean includeManual,
            boolean includeReview,
            boolean nextMonth,
            List<UUID> selectedTransactionIds
    ) {
        return new BudgetPlanningSuggestionPreviewRequest(
                2026,
                5,
                mode,
                includeImportedOnly,
                includeManual,
                includeReview,
                target,
                nextMonth,
                selectedTransactionIds,
                new BigDecimal("100")
        );
    }

    private BudgetPlanningSuggestionCommitRequest commitBudget(
            UUID categoryId,
            String amount,
            boolean overwrite
    ) {
        return new BudgetPlanningSuggestionCommitRequest(
                2026,
                5,
                List.of(new ApplyBudgetSuggestion(
                        categoryId,
                        new BigDecimal(amount),
                        true,
                        false,
                        false,
                        null
                )),
                List.of(),
                true,
                overwrite
        );
    }

    private ApplyMonthlyPlanSuggestion planSuggestion(
            UUID categoryId,
            String title,
            String amount,
            MonthlyPlanItem.Source source
    ) {
        return new ApplyMonthlyPlanSuggestion(
                title,
                null,
                LocalDate.of(2026, 6, 5),
                2026,
                6,
                new BigDecimal(amount),
                null,
                null,
                categoryId,
                accountId,
                MonthlyPlanItem.Type.EXPENSE,
                MonthlyPlanItem.Priority.ESSENTIAL,
                source,
                true,
                false,
                List.of()
        );
    }

    private BudgetPlanningSuggestionCommitRequest commitPlan(ApplyMonthlyPlanSuggestion suggestion) {
        return new BudgetPlanningSuggestionCommitRequest(
                2026,
                5,
                List.of(),
                List.of(suggestion),
                true,
                false
        );
    }

    private MonthlyPlanItem existingPlan(
            UUID categoryId,
            String title,
            String amount,
            MonthlyPlanItem.Source source,
            MonthlyPlanItem.Status status
    ) {
        return MonthlyPlanItem.builder()
                .profileId(profileId)
                .categoryId(categoryId)
                .title(title)
                .periodYear(2026)
                .periodMonth(6)
                .amount(new BigDecimal(amount))
                .type(MonthlyPlanItem.Type.EXPENSE)
                .source(source)
                .status(status)
                .build();
    }
}
