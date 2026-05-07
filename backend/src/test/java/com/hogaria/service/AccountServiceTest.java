package com.hogaria.service;
import com.hogaria.dto.AccountDtos.AccountCreateRequest;import com.hogaria.entity.Account;import com.hogaria.exception.ForbiddenException;import com.hogaria.repository.*;import org.junit.jupiter.api.Test;import org.junit.jupiter.api.extension.ExtendWith;import org.mockito.*;import org.mockito.junit.jupiter.MockitoExtension;import java.util.*;import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class AccountServiceTest { @Mock AccountRepository accountRepository; @Mock FinancialProfileRepository profileRepository; @InjectMocks AccountService service;
@Test void createsValid(){var u=UUID.randomUUID();var p=UUID.randomUUID();when(profileRepository.findByIdAndUserId(p,u)).thenReturn(Optional.of(new com.hogaria.entity.FinancialProfile()));var req=new AccountCreateRequest("Caja", Account.AccountType.CASH,"ars",null,null,null); when(accountRepository.save(any())).thenAnswer(i->i.getArgument(0)); assertEquals("ARS",service.create(u,p,req).currency());}
@Test void rejectsOtherProfile(){assertThrows(ForbiddenException.class,()->service.list(UUID.randomUUID(),UUID.randomUUID()));}
@Test void deactivates(){var u=UUID.randomUUID();var p=UUID.randomUUID();var a=Account.builder().id(UUID.randomUUID()).profileId(p).active(true).build();when(accountRepository.findById(a.getId())).thenReturn(Optional.of(a));when(profileRepository.findByIdAndUserId(p,u)).thenReturn(Optional.of(new com.hogaria.entity.FinancialProfile()));service.deactivate(u,a.getId());assertFalse(a.getActive());}
}
