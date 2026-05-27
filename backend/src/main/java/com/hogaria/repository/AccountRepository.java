package com.hogaria.repository;
import com.hogaria.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findByProfileIdAndActiveTrue(UUID profileId);
    Optional<Account> findByIdAndProfileId(UUID id, UUID profileId);
    boolean existsByIdAndProfileId(UUID id, UUID profileId);
    boolean existsByProfileIdAndAccountKeyAndCurrencyAndActiveTrue(UUID profileId, String accountKey, String currency);

    @Query("""
            select count(a) > 0 from Account a
            where a.profileId = :profileId
              and a.accountKey = :accountKey
              and a.currency = :currency
              and a.active = true
              and (:excludeId is null or a.id <> :excludeId)
            """)
    boolean existsActiveDuplicateKey(
            @Param("profileId") UUID profileId,
            @Param("accountKey") String accountKey,
            @Param("currency") String currency,
            @Param("excludeId") UUID excludeId
    );
}
