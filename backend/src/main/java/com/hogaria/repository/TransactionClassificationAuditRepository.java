package com.hogaria.repository;

import com.hogaria.entity.TransactionClassificationAudit;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionClassificationAuditRepository extends JpaRepository<TransactionClassificationAudit, UUID> {
}
