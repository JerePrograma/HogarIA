package com.hogaria.controller;
import com.hogaria.dto.AccountDtos.*;import com.hogaria.security.CurrentUserResolver;import com.hogaria.service.AccountService;import jakarta.validation.Valid;import java.util.*;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api")
public class AccountController {
  private final AccountService service; private final CurrentUserResolver parser;
  public AccountController(AccountService service, CurrentUserResolver parser){this.service=service;this.parser=parser;}
  @PostMapping("/profiles/{profileId}/accounts") public AccountResponse create(@RequestHeader("X-User-Id") String h,@PathVariable UUID profileId,@Valid @RequestBody AccountCreateRequest r){return service.create(parser.parse(h),profileId,r);} 
  @GetMapping("/profiles/{profileId}/accounts") public List<AccountResponse> list(@RequestHeader("X-User-Id") String h,@PathVariable UUID profileId){return service.list(parser.parse(h),profileId);} 
  @GetMapping("/accounts/{id}") public AccountResponse get(@RequestHeader("X-User-Id") String h,@PathVariable UUID id){return service.get(parser.parse(h),id);} 
  @PutMapping("/accounts/{id}") public AccountResponse update(@RequestHeader("X-User-Id") String h,@PathVariable UUID id,@Valid @RequestBody AccountUpdateRequest r){return service.update(parser.parse(h),id,r);} 
  @DeleteMapping("/accounts/{id}") public void deactivate(@RequestHeader("X-User-Id") String h,@PathVariable UUID id){service.deactivate(parser.parse(h),id);} }
