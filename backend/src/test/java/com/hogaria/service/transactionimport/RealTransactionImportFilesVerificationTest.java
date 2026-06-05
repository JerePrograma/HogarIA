package com.hogaria.service.transactionimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.entity.Category;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import com.hogaria.repository.TransactionImportReferenceRepository;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class RealTransactionImportFilesVerificationTest {

    private static Stream<ExpectedFile> files() {
        return Stream.of(
                new ExpectedFile("Provincia Enero.xlsx", TransactionImportSource.BANCO_PROVINCIA, "BANCO_PROVINCIA_MOVIMIENTOS", 20, "808925.83", "133734.61"),
                new ExpectedFile("Provincia Febrero.xlsx", TransactionImportSource.BANCO_PROVINCIA, "BANCO_PROVINCIA_MOVIMIENTOS", 20, "640382.14", "215823.02"),
                new ExpectedFile("Provincia Marzo.xlsx", TransactionImportSource.BANCO_PROVINCIA, "BANCO_PROVINCIA_MOVIMIENTOS", 20, "497974.70", "887382.79"),
                new ExpectedFile("Provincia Abril.xlsx", TransactionImportSource.BANCO_PROVINCIA, "BANCO_PROVINCIA_MOVIMIENTOS", 20, "658293.91", "452975.04"),
                new ExpectedFile("Provincia Mayo.xlsx", TransactionImportSource.BANCO_PROVINCIA, "BANCO_PROVINCIA_MOVIMIENTOS", 20, "599427.86", "610533.46"),
                new ExpectedFile("MercadoPago Enero-Marzo.xlsx", TransactionImportSource.MERCADO_PAGO, "MERCADO_PAGO_SETTLEMENT", 70, "1246769.90", "384634.77"),
                new ExpectedFile("MercadoPago Abril-Mayo.xlsx", TransactionImportSource.MERCADO_PAGO, "MERCADO_PAGO_SETTLEMENT", 106, "1337178.94", "592197.00")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("files")
    void verifiesRealFileEndToEndPreviewSemantics(ExpectedFile expected) throws Exception {
        var directory = System.getProperty("transactionImportRealFilesDir", "");
        Assumptions.assumeFalse(directory.isBlank(), "Set -DtransactionImportRealFilesDir to run real-file verification");
        var path = Path.of(directory, expected.fileName());
        Assumptions.assumeTrue(Files.isRegularFile(path), "Missing real file: " + path);

        var bytes = Files.readAllBytes(path);
        var normalizer = new ImportTextNormalizer();
        var classifier = new TransactionImportRuleClassifier(normalizer);
        var hashService = new ImportSourceHashService();
        var mapper = new ObjectMapper().findAndRegisterModules();
        var detector = new TransactionExcelImportFormatDetector(normalizer);
        var detection = detector.detect(bytes);
        var profileId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var accountId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        TransactionExcelMovementParser parser = detection.template() == ExcelImportTemplate.BANCO_PROVINCIA_MOVIMIENTOS
                ? new BancoProvinciaMovimientosExcelParser(normalizer, classifier, hashService, mapper)
                : new MercadoPagoSettlementExcelParser(normalizer, classifier, hashService, mapper);

        var candidates = parser.parse(bytes, detection, profileId, accountId);
        var secondPass = parser.parse(bytes, detection, profileId, accountId);

        assertEquals(expected.format(), detection.displayName());
        assertEquals(expected.source(), detection.template().source());
        assertEquals(expected.rows(), candidates.size());
        assertEquals(candidates.stream().map(ImportedMovementCandidate::sourceHash).toList(),
                secondPass.stream().map(ImportedMovementCandidate::sourceHash).toList());

        for (var candidate : candidates) {
            if (candidate.rowStatus() == RowStatus.ERROR || candidate.rowStatus() == RowStatus.SKIPPED) {
                assertFalse(candidate.warning() == null || candidate.warning().isBlank());
                continue;
            }
            assertNotNull(candidate.realDate());
            assertNotNull(candidate.signedAmount());
            assertNotNull(candidate.amountAbs());
            assertTrue(candidate.amountAbs().signum() > 0);
            assertEquals(0, candidate.signedAmount().abs().compareTo(candidate.amountAbs()));
            assertFalse(candidate.sourceHash() == null || candidate.sourceHash().isBlank());
            assertFalse(candidate.rawJson() == null || candidate.rawJson().isBlank());
            assertNotNull(candidate.movementType());
            assertNotNull(candidate.balanceImpact());
        }

        assertAmount(expected.positive(), candidates.stream()
                .map(ImportedMovementCandidate::signedAmount)
                .filter(amount -> amount != null && amount.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        assertAmount(expected.negative(), candidates.stream()
                .map(ImportedMovementCandidate::signedAmount)
                .filter(amount -> amount != null && amount.signum() < 0)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        var categoryRepository = org.mockito.Mockito.mock(CategoryRepository.class);
        var categories = categoriesFor(candidates);
        when(categoryRepository.findByProfileIdAndActiveTrue(profileId)).thenReturn(List.of());
        when(categoryRepository.findByProfileIdIsNullAndActiveTrue()).thenReturn(categories);
        var categoryResolver = new TransactionImportCategoryResolver(categoryRepository);
        var previewMapper = new TransactionImportPreviewMapper(categoryRepository, categoryResolver);
        var previewRows = candidates.stream().map(candidate -> previewMapper.toPreviewRow(profileId, candidate)).toList();
        var duplicateDetector = new TransactionImportDuplicateDetector(
                org.mockito.Mockito.mock(MoneyTransactionRepository.class),
                org.mockito.Mockito.mock(TransactionImportReferenceRepository.class),
                categoryRepository
        );
        var resolvedRows = duplicateDetector.applyDuplicateStatus(profileId, accountId, previewRows);
        var summary = new TransactionImportSummaryFactory(categoryResolver)
                .summarize(UUID.randomUUID(), expected.source(), accountId, resolvedRows);

        assertEquals(expected.rows(), summary.totalRows());
        assertEquals(0, summary.errorRows());
        assertTrue(summary.importableRows() + summary.blockedRows() >= summary.totalRows());
        assertTrue(summary.internalTransferRows() <= summary.reviewRows() + summary.importableRows());
        assertTrue(summary.duplicateRows() <= summary.totalRows());

        System.out.printf(
                "REAL_IMPORT_REPORT|%s|%s|%s|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%s|%s|%s%n",
                expected.fileName(),
                expected.source(),
                expected.format(),
                summary.totalRows(),
                summary.importableRows(),
                summary.duplicateRows(),
                summary.internalTransferRows(),
                summary.technicalNeutralRows(),
                summary.needsCategoryRows(),
                summary.blockingCategoryRows(),
                summary.reviewRows(),
                summary.errorRows(),
                summary.skippedRows(),
                expected.positive(),
                expected.negative(),
                impactBreakdown(resolvedRows)
        );
    }

    private List<Category> categoriesFor(List<ImportedMovementCandidate> candidates) {
        var categories = new ArrayList<Category>();
        var seen = new java.util.HashSet<String>();

        for (var candidate : candidates) {
            var key = ImportTextSupport.firstNonBlank(candidate.categorySuggestionKey(), candidate.categorySuggestionName());
            if (key.isBlank() || !seen.add(key)) {
                continue;
            }

            categories.add(Category.builder()
                    .id(UUID.randomUUID())
                    .name(ImportTextSupport.firstNonBlank(candidate.categorySuggestionName(), key))
                    .categoryKey(candidate.categorySuggestionKey())
                    .type(categoryType(candidate.movementType()))
                    .active(true)
                    .build());
        }

        return categories;
    }

    private Category.Type categoryType(MoneyTransaction.MovementType movementType) {
        return switch (movementType) {
            case INCOME -> Category.Type.INCOME;
            case SAVING -> Category.Type.INVESTMENT;
            case TRANSFER -> Category.Type.SAVING;
            case EXPENSE, ADJUSTMENT -> Category.Type.VARIABLE_EXPENSE;
        };
    }

    private String impactBreakdown(List<TransactionImportPreviewRow> rows) {
        var totals = new LinkedHashMap<MoneyTransaction.BalanceImpact, BigDecimal>();
        for (var row : rows) {
            totals.merge(
                    row.balanceImpact() == null ? MoneyTransaction.BalanceImpact.UNKNOWN : row.balanceImpact(),
                    row.rawSignedAmount() == null ? BigDecimal.ZERO : row.rawSignedAmount(),
                    BigDecimal::add
            );
        }
        return totals.toString().replace("|", "/");
    }

    private void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "amount");
    }

    private record ExpectedFile(
            String fileName,
            TransactionImportSource source,
            String format,
            int rows,
            String positive,
            String negative
    ) {
        @Override
        public String toString() {
            return fileName;
        }
    }
}
