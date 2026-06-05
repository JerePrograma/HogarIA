package com.hogaria.service;

import static org.mockito.Mockito.verify;

import com.hogaria.dto.TransactionImportDtos.TransactionImportCommitRequest;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.service.transactionimport.TransactionImportCommitService;
import com.hogaria.service.transactionimport.TransactionImportPreviewService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class TransactionImportServiceTest {

    @Test
    void remainsAThinFacadeOverPreviewAndCommitServices() {
        var previewService = org.mockito.Mockito.mock(TransactionImportPreviewService.class);
        var commitService = org.mockito.Mockito.mock(TransactionImportCommitService.class);
        var service = new TransactionImportService(previewService, commitService);
        var userId = UUID.randomUUID();
        var profileId = UUID.randomUUID();
        var accountId = UUID.randomUUID();
        var batchId = UUID.randomUUID();
        var file = new MockMultipartFile("file", "movimientos.xlsx", "application/octet-stream", new byte[]{1});
        var request = new TransactionImportCommitRequest(List.of(), false, true);

        service.preview(userId, profileId, accountId, TransactionImportSource.AUTO, file, 2026, 5);
        service.getBatch(userId, profileId, batchId);
        service.commit(userId, profileId, batchId, request);

        verify(previewService).preview(userId, profileId, accountId, TransactionImportSource.AUTO, file, 2026, 5);
        verify(previewService).getBatch(userId, profileId, batchId);
        verify(commitService).commit(userId, profileId, batchId, request);
    }
}
