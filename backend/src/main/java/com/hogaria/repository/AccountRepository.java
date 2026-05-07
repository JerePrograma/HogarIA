package com.hogaria.repository;
import com.hogaria.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AccountRepository extends JpaRepository<Account, UUID> { List<Account> findByProfileIdAndActiveTrue(UUID profileId); Optional<Account> findByIdAndProfileId(UUID id, UUID profileId); boolean existsByIdAndProfileId(UUID id, UUID profileId); }
