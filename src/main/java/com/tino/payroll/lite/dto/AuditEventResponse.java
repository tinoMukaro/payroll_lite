package com.tino.payroll.lite.dto;

import com.tino.payroll.lite.enums.AuditAction;
import com.tino.payroll.lite.enums.AuditEntityType;
import com.tino.payroll.lite.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AuditEventResponse {
    private Long id;
    private Long actorUserId;
    private String actorEmail;
    private Role actorRole;
    private AuditAction action;
    private AuditEntityType entityType;
    private Long entityId;
    private String details;
    private Instant occurredAt;
}
