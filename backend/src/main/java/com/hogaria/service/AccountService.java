package com.hogaria.service;

import com.hogaria.dto.AccountDtos.*;
import com.hogaria.entity.Account;
import com.hogaria.exception.*;
import com.hogaria.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
  private final AccountRepository accountRepository; private final FinancialProfileRepository profileRepository;
  public AccountService(AccountRepository accountRepository, FinancialProfileRepository profileRepository){this.accountRepository=accountRepository;this.profileRepository=profileRepository;}
  public AccountResponse create(UUID userId, UUID profileId, AccountCreateRequest r){ensureProfile(userId,profileId); var a=Account.builder().profileId(profileId).name(r.name()).accountType(r.accountType()).currency(r.currency().toUpperCase()).creditLimit(r.creditLimit()).statementCloseDay(r.statementCloseDay()).dueDay(r.dueDay()).active(true).build(); return toResponse(accountRepository.save(a));}
  public List<AccountResponse> list(UUID userId, UUID profileId){ensureProfile(userId,profileId); return accountRepository.findByProfileIdAndActiveTrue(profileId).stream().map(this::toResponse).toList();}
  public AccountResponse get(UUID userId, UUID accountId){var a=accountRepository.findById(accountId).orElseThrow(()->new NotFoundException("Account not found")); ensureProfile(userId,a.getProfileId()); return toResponse(a);}  
  public AccountResponse update(UUID userId, UUID accountId, AccountUpdateRequest r){var a=accountRepository.findById(accountId).orElseThrow(()->new NotFoundException("Account not found")); ensureProfile(userId,a.getProfileId()); if(r.name()!=null)a.setName(r.name()); if(r.accountType()!=null)a.setAccountType(r.accountType()); if(r.currency()!=null)a.setCurrency(r.currency().toUpperCase()); if(r.creditLimit()!=null)a.setCreditLimit(r.creditLimit()); if(r.statementCloseDay()!=null)a.setStatementCloseDay(r.statementCloseDay()); if(r.dueDay()!=null)a.setDueDay(r.dueDay()); if(r.active()!=null)a.setActive(r.active()); return toResponse(accountRepository.save(a));}
  public void deactivate(UUID userId, UUID accountId){var a=accountRepository.findById(accountId).orElseThrow(()->new NotFoundException("Account not found")); ensureProfile(userId,a.getProfileId()); a.setActive(false); accountRepository.save(a);}  
  private void ensureProfile(UUID userId, UUID profileId){profileRepository.findByIdAndUserId(profileId,userId).orElseThrow(()->new ForbiddenException("Profile does not belong to user"));}
  private AccountResponse toResponse(Account a){return new AccountResponse(a.getId(),a.getProfileId(),a.getName(),a.getAccountType(),a.getCurrency(),a.getCreditLimit(),a.getStatementCloseDay(),a.getDueDay(),a.getActive(),a.getCreatedAt(),a.getUpdatedAt());}
}
