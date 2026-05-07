package com.hogaria.controller;
import com.hogaria.dto.*;import com.hogaria.security.CurrentUserResolver;import com.hogaria.service.TransactionService;import jakarta.validation.Valid;import java.util.*;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api")
public class TransactionController {
  private final TransactionService service; private final CurrentUserResolver parser;
  public TransactionController(TransactionService service, CurrentUserResolver parser){this.service=service;this.parser=parser;}
  @PostMapping("/transactions") public TransactionResponse create(@RequestHeader("X-User-Id") String h,@Valid @RequestBody TransactionCreateRequest r){return service.create(r,parser.parse(h));}
  @GetMapping("/profiles/{profileId}/transactions") public List<TransactionResponse> list(@RequestHeader("X-User-Id") String h,@PathVariable UUID profileId,@RequestParam int year,@RequestParam int month){return service.list(parser.parse(h),profileId,year,month);} 
  @GetMapping("/transactions/{id}") public TransactionResponse get(@RequestHeader("X-User-Id") String h,@PathVariable UUID id){return service.get(parser.parse(h),id);} 
  @PutMapping("/transactions/{id}") public TransactionResponse update(@RequestHeader("X-User-Id") String h,@PathVariable UUID id,@Valid @RequestBody TransactionUpdateRequest r){return service.update(parser.parse(h),id,r);} 
  @DeleteMapping("/transactions/{id}") public void delete(@RequestHeader("X-User-Id") String h,@PathVariable UUID id){service.delete(parser.parse(h),id);} }
