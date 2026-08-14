package com.tino.payroll.lite.repository;

import com.tino.payroll.lite.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditEventRepository extends
        JpaRepository<AuditEvent, Long>,
        JpaSpecificationExecutor<AuditEvent> {
}
