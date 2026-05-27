package com.hogaria.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hogaria.config.SecurityConfig;
import com.hogaria.dto.TransactionDeletionResponse;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.exception.DomainConflictException;
import com.hogaria.exception.ErrorResponse;
import com.hogaria.security.CurrentUserResolver;
import com.hogaria.security.JwtAuthenticationFilter;
import com.hogaria.security.JwtService;
import com.hogaria.service.TransactionService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TransactionController.class)
@Import({SecurityConfig.class, CurrentUserResolver.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {"app.jwt.secret=test-secret", "app.security.allow-x-user-id-fallback=false"})
class TransactionControllerWebMvcTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    @MockBean private TransactionService transactionService;

    @Test
    void deleteReturnsExplicitSoftIgnoreContract() throws Exception {
        var userId = UUID.randomUUID();
        var transactionId = UUID.randomUUID();
        when(transactionService.delete(eq(userId), eq(transactionId)))
                .thenReturn(new TransactionDeletionResponse(
                        transactionId,
                        TransactionDeletionResponse.Mode.SOFT_IGNORE,
                        "IMPORTED_TRANSACTION_SOFT_IGNORED",
                        "Movimiento ignorado para preservar trazabilidad.",
                        1,
                        2,
                        0,
                        MoneyTransaction.Status.IGNORED,
                        MoneyTransaction.ClassificationStatus.IGNORED_BY_RULE
                ));

        mockMvc.perform(delete("/api/transactions/{id}", transactionId)
                        .header("Authorization", "Bearer " + token(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$.mode").value("SOFT_IGNORE"))
                .andExpect(jsonPath("$.code").value("IMPORTED_TRANSACTION_SOFT_IGNORED"))
                .andExpect(jsonPath("$.message").value("Movimiento ignorado para preservar trazabilidad."))
                .andExpect(jsonPath("$.linkedItemsUpdated").value(1))
                .andExpect(jsonPath("$.matchesDeleted").value(2))
                .andExpect(jsonPath("$.resultingStatus").value("IGNORED"))
                .andExpect(jsonPath("$.resultingClassificationStatus").value("IGNORED_BY_RULE"));
    }

    @Test
    void validationErrorReturnsBadRequestWithDetails() throws Exception {
        var userId = UUID.randomUUID();

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void malformedRequestReturnsBadRequest() throws Exception {
        var userId = UUID.randomUUID();

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void domainConflictReturnsSpecificConflictCode() throws Exception {
        var userId = UUID.randomUUID();
        var transactionId = UUID.randomUUID();
        when(transactionService.delete(eq(userId), eq(transactionId)))
                .thenThrow(new DomainConflictException(
                        "No se puede eliminar.",
                        "TRANSACTION_DELETE_BLOCKED",
                        List.of(new ErrorResponse.Detail("transactionId", transactionId.toString()))
                ));

        mockMvc.perform(delete("/api/transactions/{id}", transactionId)
                        .header("Authorization", "Bearer " + token(userId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRANSACTION_DELETE_BLOCKED"))
                .andExpect(jsonPath("$.details[0].field").value("transactionId"));
    }

    @Test
    void dataIntegrityConflictDoesNotExposeTechnicalMessage() throws Exception {
        var userId = UUID.randomUUID();
        var transactionId = UUID.randomUUID();
        when(transactionService.delete(eq(userId), eq(transactionId)))
                .thenThrow(new DataIntegrityViolationException(
                        "ERROR: update or delete violates foreign key constraint \"fk_monthly_plan_tx_match_tx\""
                ));

        mockMvc.perform(delete("/api/transactions/{id}", transactionId)
                        .header("Authorization", "Bearer " + token(userId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DATA_INTEGRITY_CONFLICT"))
                .andExpect(jsonPath("$.message").value("La operación no puede completarse porque existen datos relacionados."))
                .andExpect(jsonPath("$.details[0].field").value("constraint"))
                .andExpect(jsonPath("$.details[0].message").value("fk_monthly_plan_tx_match_tx"));
    }

    private String token(UUID userId) {
        return jwtService.generateToken(userId, "test@hogaria.local");
    }
}
