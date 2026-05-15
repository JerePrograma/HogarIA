package com.hogaria.controller;

import com.hogaria.dto.TransactionImportDtos.*;
import com.hogaria.security.CurrentUserResolver;
import com.hogaria.service.TransactionImportService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequestMapping("/api/profiles/{profileId}/transaction-imports")
public class TransactionImportController {
  private final TransactionImportService service; private final CurrentUserResolver parser;
  public TransactionImportController(TransactionImportService s, CurrentUserResolver p){service=s;parser=p;}
  @PostMapping("/preview") public TransactionImportPreviewResponse preview(@RequestHeader("X-User-Id") String h,@PathVariable UUID profileId,@RequestParam TransactionImportSource source,@RequestParam UUID accountId,@RequestParam(required=false) Integer year,@RequestParam(required=false) Integer month,@RequestPart("file") MultipartFile file){return service.preview(parser.parse(h),profileId,accountId,source,file,year,month);} 
  @GetMapping("/{batchId}") public TransactionImportPreviewResponse getBatch(@RequestHeader("X-User-Id") String h,@PathVariable UUID profileId,@PathVariable UUID batchId){return service.getBatch(parser.parse(h),profileId,batchId);} 
  @PostMapping("/{batchId}/commit") public TransactionImportCommitResponse commit(@RequestHeader("X-User-Id") String h,@PathVariable UUID profileId,@PathVariable UUID batchId,@Valid @RequestBody TransactionImportCommitRequest request){return service.commit(parser.parse(h),profileId,batchId,request);} 
}
