package com.hogaria.service;

import com.hogaria.dto.ProfileDtos.*;
import com.hogaria.entity.FinancialProfile;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.exception.NotFoundException;
import com.hogaria.repository.FinancialProfileRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {
  private final FinancialProfileRepository repo;
  public ProfileService(FinancialProfileRepository repo){this.repo=repo;}
  public ProfileResponse create(UUID userId, ProfileCreateRequest r){ return to(repo.save(FinancialProfile.builder().userId(userId).name(r.name()).type(r.type()).baseCurrency(r.baseCurrency()).activeYear(r.activeYear()).active(true).build())); }
  public List<ProfileResponse> list(UUID userId){ return repo.findByUserIdAndActiveTrue(userId).stream().map(this::to).toList(); }
  public ProfileResponse get(UUID userId, UUID id){ return to(repo.findById(id).filter(p->p.getUserId().equals(userId)).orElseThrow(()->new ForbiddenException("Profile does not belong to user"))); }
  public ProfileResponse update(UUID userId, UUID id, ProfileUpdateRequest r){ var p=repo.findById(id).orElseThrow(()->new NotFoundException("Profile not found")); if(!p.getUserId().equals(userId)) throw new ForbiddenException("Profile does not belong to user"); if(r.name()!=null)p.setName(r.name()); if(r.type()!=null)p.setType(r.type()); if(r.baseCurrency()!=null)p.setBaseCurrency(r.baseCurrency()); if(r.activeYear()!=null)p.setActiveYear(r.activeYear()); if(r.active()!=null)p.setActive(r.active()); return to(repo.save(p)); }
  public void delete(UUID userId, UUID id){ var p=repo.findById(id).orElseThrow(()->new NotFoundException("Profile not found")); if(!p.getUserId().equals(userId)) throw new ForbiddenException("Profile does not belong to user"); p.setActive(false); repo.save(p); }
  private ProfileResponse to(FinancialProfile p){ return new ProfileResponse(p.getId(),p.getName(),p.getType(),p.getBaseCurrency(),p.getActiveYear(),p.getActive(),p.getCreatedAt(),p.getUpdatedAt());}
}
