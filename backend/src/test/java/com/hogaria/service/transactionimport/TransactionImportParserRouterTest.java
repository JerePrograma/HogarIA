package com.hogaria.service.transactionimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.entity.MoneyTransaction;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class TransactionImportParserRouterTest {

    @Test
    void autoFallsBackToMercadoPagoDelimitedForNonExcelFiles() {
        var delimited = org.mockito.Mockito.mock(MercadoPagoDelimitedImportParser.class);
        var row = TransactionImportTestData.row(
                1, RowStatus.READY, MoneyTransaction.MovementType.EXPENSE,
                MoneyTransaction.BalanceImpact.CONSUMPTION_EXPENSE,
                MoneyTransaction.ClassificationStatus.CLASSIFIED, UUID.randomUUID()
        );
        var profileId = UUID.randomUUID();
        var accountId = UUID.randomUUID();
        var file = new MockMultipartFile("file", "movimientos.csv", "text/csv", "header\nrow".getBytes());
        when(delimited.parse(any(), eq(profileId), eq(accountId), eq(2026), eq(5))).thenReturn(List.of(row));
        var router = router(List.of(), delimited);

        var rows = router.parse(TransactionImportSource.AUTO, file, profileId, accountId, 2026, 5);

        assertEquals(List.of(row), rows);
        verify(delimited).parse(any(), eq(profileId), eq(accountId), eq(2026), eq(5));
    }

    @Test
    void autoDetectsAndRoutesSupportedExcelTemplates() {
        var detector = org.mockito.Mockito.mock(TransactionExcelImportFormatDetector.class);
        var parser = org.mockito.Mockito.mock(TransactionExcelMovementParser.class);
        var previewMapper = org.mockito.Mockito.mock(TransactionImportPreviewMapper.class);
        var profileId = UUID.randomUUID();
        var accountId = UUID.randomUUID();
        var detection = new DetectedExcelImportFormat(
                ExcelImportTemplate.BANCO_PROVINCIA_MOVIMIENTOS,
                "Hoja 1",
                1,
                List.of("Número Secuencia", "Fecha"),
                Map.of()
        );
        var candidate = ImportedMovementCandidate.builder()
                .source(TransactionImportSource.BANCO_PROVINCIA)
                .rowNumber(3)
                .build();
        var row = TransactionImportTestData.row(
                3, RowStatus.READY, MoneyTransaction.MovementType.EXPENSE,
                MoneyTransaction.BalanceImpact.CONSUMPTION_EXPENSE,
                MoneyTransaction.ClassificationStatus.CLASSIFIED, UUID.randomUUID()
        );
        when(detector.detect(any())).thenReturn(detection);
        when(parser.template()).thenReturn(ExcelImportTemplate.BANCO_PROVINCIA_MOVIMIENTOS);
        when(parser.parse(any(), eq(detection), eq(profileId), eq(accountId))).thenReturn(List.of(candidate));
        when(previewMapper.toPreviewRow(profileId, candidate)).thenReturn(row);
        var router = new TransactionImportParserRouter(
                detector,
                List.of(parser),
                previewMapper,
                org.mockito.Mockito.mock(MercadoPagoDelimitedImportParser.class),
                org.mockito.Mockito.mock(GenericCardImportParser.class)
        );
        var file = new MockMultipartFile(
                "file",
                "provincia.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{'P', 'K', 1}
        );

        var rows = router.parse(TransactionImportSource.AUTO, file, profileId, accountId, null, null);

        assertEquals(List.of(row), rows);
        verify(parser).parse(any(), eq(detection), eq(profileId), eq(accountId));
    }

    private TransactionImportParserRouter router(
            List<TransactionExcelMovementParser> excelParsers,
            MercadoPagoDelimitedImportParser delimited
    ) {
        return new TransactionImportParserRouter(
                org.mockito.Mockito.mock(TransactionExcelImportFormatDetector.class),
                excelParsers,
                org.mockito.Mockito.mock(TransactionImportPreviewMapper.class),
                delimited,
                org.mockito.Mockito.mock(GenericCardImportParser.class)
        );
    }
}
