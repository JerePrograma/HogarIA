package com.hogaria.service;
import com.hogaria.domains.transactions.lifecycle.TransactionLifecycleService;import com.hogaria.dto.TransactionCreateRequest;import com.hogaria.entity.MoneyTransaction;import com.hogaria.exception.BadRequestException;import com.hogaria.repository.*;import org.junit.jupiter.api.Test;import org.junit.jupiter.api.extension.ExtendWith;import org.mockito.*;import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;import java.time.LocalDate;import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
 @Mock MoneyTransactionRepository txRepo; @Mock FinancialProfileRepository profileRepo; @Mock AccountRepository accountRepo; @Mock CategoryRepository categoryRepo; @Mock TransactionCategorySuggestionService suggestionService; @Mock TransactionLifecycleService transactionLifecycleService; @InjectMocks TransactionService service;
 @Test void rejectsAmountZero(){ UUID id=UUID.randomUUID(); var req=new TransactionCreateRequest(id,id,id, MoneyTransaction.MovementType.EXPENSE, LocalDate.now(),LocalDate.now(),BigDecimal.ZERO,"ARS",null,null,null); assertThrows(BadRequestException.class,()->service.create(req,id)); }
}
