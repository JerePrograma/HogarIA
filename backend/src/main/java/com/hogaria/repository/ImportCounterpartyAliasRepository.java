package com.hogaria.repository;

import com.hogaria.entity.ImportCounterpartyAlias;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportCounterpartyAliasRepository extends JpaRepository<ImportCounterpartyAlias, UUID> {
    List<ImportCounterpartyAlias> findByProfileIdAndActiveTrue(UUID profileId);
}
