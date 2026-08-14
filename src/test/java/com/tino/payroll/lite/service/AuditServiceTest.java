package com.tino.payroll.lite.service;

import com.tino.payroll.lite.entity.AuditEvent;
import com.tino.payroll.lite.entity.User;
import com.tino.payroll.lite.enums.AuditAction;
import com.tino.payroll.lite.enums.AuditEntityType;
import com.tino.payroll.lite.enums.Role;
import com.tino.payroll.lite.repository.AuditEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository repository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsAuthenticatedActorAsSnapshot() {
        User actor = User.builder()
                .id(7L)
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, null)
        );
        AuditService service = new AuditService(repository);

        service.record(
                AuditAction.EMPLOYEE_CREATED,
                AuditEntityType.EMPLOYEE,
                42L,
                "Created employee EMP-000042"
        );

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(captor.capture());
        AuditEvent event = captor.getValue();
        assertEquals(7L, event.getActorUserId());
        assertEquals("admin@example.com", event.getActorEmail());
        assertEquals(Role.ADMIN, event.getActorRole());
        assertEquals(AuditAction.EMPLOYEE_CREATED, event.getAction());
        assertEquals(AuditEntityType.EMPLOYEE, event.getEntityType());
        assertEquals(42L, event.getEntityId());
    }

    @Test
    void systemEventsDoNotPretendToBeAUser() {
        AuditService service = new AuditService(repository);

        service.recordSystem(
                AuditAction.ADMIN_BOOTSTRAPPED,
                AuditEntityType.USER,
                1L,
                "Bootstrapped initial administrator"
        );

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(captor.capture());
        assertEquals("SYSTEM", captor.getValue().getActorEmail());
        assertEquals(null, captor.getValue().getActorUserId());
        assertEquals(null, captor.getValue().getActorRole());
    }

    @Test
    void searchesWithSpecificationsSoMissingFiltersAreNotBoundAsUnknownSqlTypes() {
        AuditService service = new AuditService(repository);
        when(repository.findAll(
                org.mockito.ArgumentMatchers.<Specification<AuditEvent>>any(),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        service.search(null, null, null, null, null, 0, 25);

        verify(repository).findAll(
                org.mockito.ArgumentMatchers.<Specification<AuditEvent>>any(),
                any(Pageable.class)
        );
    }
}
