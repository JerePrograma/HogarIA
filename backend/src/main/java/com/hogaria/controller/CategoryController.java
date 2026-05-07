package com.hogaria.controller;
import com.hogaria.dto.CategoryDtos.*;import com.hogaria.security.CurrentUserResolver;import com.hogaria.service.CategoryService;import jakarta.validation.Valid;import java.util.*;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api")
public class CategoryController {
  private final CategoryService service; private final CurrentUserResolver parser;
  public CategoryController(CategoryService service, CurrentUserResolver parser){this.service=service;this.parser=parser;}
  @PostMapping("/profiles/{profileId}/categories") public CategoryResponse create(@RequestHeader("X-User-Id") String h,@PathVariable UUID profileId,@Valid @RequestBody CategoryCreateRequest r){return service.create(parser.parse(h),profileId,r);} 
  @GetMapping("/profiles/{profileId}/categories") public List<CategoryResponse> list(@RequestHeader("X-User-Id") String h,@PathVariable UUID profileId,@RequestParam(defaultValue = "false") boolean includeGlobal){return service.list(parser.parse(h),profileId,includeGlobal);} 
  @GetMapping("/categories/{id}") public CategoryResponse get(@RequestHeader("X-User-Id") String h,@PathVariable UUID id){return service.get(parser.parse(h),id);} 
  @PutMapping("/categories/{id}") public CategoryResponse update(@RequestHeader("X-User-Id") String h,@PathVariable UUID id,@Valid @RequestBody CategoryUpdateRequest r){return service.update(parser.parse(h),id,r);} 
  @DeleteMapping("/categories/{id}") public void deactivate(@RequestHeader("X-User-Id") String h,@PathVariable UUID id){service.deactivate(parser.parse(h),id);} }
