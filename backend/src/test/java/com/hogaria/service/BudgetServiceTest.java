package com.hogaria.service;
import com.hogaria.dto.BudgetDtos.*;import com.hogaria.entity.*;import com.hogaria.exception.ForbiddenException;import com.hogaria.repository.*;import java.math.BigDecimal;import java.util.*;import org.junit.jupiter.api.Test;import org.junit.jupiter.api.extension.ExtendWith;import org.mockito.*;import org.mockito.junit.jupiter.MockitoExtension;import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class BudgetServiceTest { @Mock FinancialProfileRepository pr; @Mock BudgetYearRepository byr; @Mock BudgetMonthRepository bmr; @Mock BudgetCategoryItemRepository bir; @Mock CategoryRepository cr; @Mock MoneyTransactionRepository tr; @InjectMocks BudgetService s;
@Test void createYear(){var u=UUID.randomUUID();var p=UUID.randomUUID();when(pr.findByIdAndUserId(p,u)).thenReturn(Optional.of(new FinancialProfile())); when(byr.existsByProfileIdAndYear(p,2026)).thenReturn(false); when(byr.save(any())).thenAnswer(a->a.getArgument(0)); assertEquals(2026,s.createBudgetYear(u,p,new BudgetYearCreateRequest(2026,BigDecimal.ONE,BigDecimal.ZERO,"n")).year());}
@Test void rejectForeignProfile(){var u=UUID.randomUUID();var p=UUID.randomUUID(); when(pr.findByIdAndUserId(p,u)).thenReturn(Optional.empty()); assertThrows(ForbiddenException.class,()->s.listBudgetYears(u,p));}
}
