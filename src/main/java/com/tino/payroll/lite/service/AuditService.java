package com.tino.payroll.lite.service;

import com.tino.payroll.lite.dto.AuditEventPageResponse;
import com.tino.payroll.lite.dto.AuditEventResponse;
import com.tino.payroll.lite.entity.AuditEvent;
import com.tino.payroll.lite.entity.User;
import com.tino.payroll.lite.enums.AuditAction;
import com.tino.payroll.lite.enums.AuditEntityType;
import com.tino.payroll.lite.enums.Role;
import com.tino.payroll.lite.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_DETAILS_LENGTH = 500;
    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final AuditEventRepository repository;

    @Transactional
    public void record(
            AuditAction action,
            AuditEntityType entityType,
            Long entityId,
            String details
    ) {
        persist(currentActor(), action, entityType, entityId, details);
    }

    @Transactional
    public void recordFor(
            User actor,
            AuditAction action,
            AuditEntityType entityType,
            Long entityId,
            String details
    ) {
        ActorSnapshot snapshot = actor == null
                ? ActorSnapshot.system()
                : new ActorSnapshot(actor.getId(), actor.getEmail(), actor.getRole());
        persist(snapshot, action, entityType, entityId, details);
    }

    @Transactional
    public void recordSystem(
            AuditAction action,
            AuditEntityType entityType,
            Long entityId,
            String details
    ) {
        persist(ActorSnapshot.system(), action, entityType, entityId, details);
    }

    @Transactional(readOnly = true)
    public AuditEventPageResponse search(
            AuditAction action,
            AuditEntityType entityType,
            String actorEmail,
            Instant fromTime,
            Instant toTime,
            int page,
            int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String normalizedActor = actorEmail == null || actorEmail.isBlank()
                ? null
                : actorEmail.trim();
        PageRequest pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"))
        );
        Page<AuditEvent> result = repository.findAll(
                buildFilters(action, entityType, normalizedActor, fromTime, toTime),
                pageable
        );
        return AuditEventPageResponse.builder()
                .content(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    private Specification<AuditEvent> buildFilters(
            AuditAction action,
            AuditEntityType entityType,
            String actorEmail,
            Instant fromTime,
            Instant toTime
    ) {
        return (root, query, builder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (action != null) {
                predicates.add(builder.equal(root.get("action"), action));
            }
            if (entityType != null) {
                predicates.add(builder.equal(root.get("entityType"), entityType));
            }
            if (actorEmail != null) {
                predicates.add(builder.like(
                        builder.lower(root.get("actorEmail")),
                        "%" + actorEmail.toLowerCase(Locale.ROOT) + "%"
                ));
            }
            if (fromTime != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("occurredAt"), fromTime));
            }
            if (toTime != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("occurredAt"), toTime));
            }
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private void persist(
            ActorSnapshot actor,
            AuditAction action,
            AuditEntityType entityType,
            Long entityId,
            String details
    ) {
        String safeDetails = details == null ? "" : details.trim();
        if (safeDetails.isBlank() || safeDetails.length() > MAX_DETAILS_LENGTH) {
            throw new IllegalArgumentException("Audit details must contain between 1 and 500 characters");
        }
        repository.save(AuditEvent.builder()
                .actorUserId(actor.userId())
                .actorEmail(actor.email())
                .actorRole(actor.role())
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(safeDetails)
                .occurredAt(Instant.now())
                .build());
    }

    private ActorSnapshot currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return new ActorSnapshot(user.getId(), user.getEmail(), user.getRole());
        }
        return ActorSnapshot.system();
    }

    private AuditEventResponse toResponse(AuditEvent event) {
        return AuditEventResponse.builder()
                .id(event.getId())
                .actorUserId(event.getActorUserId())
                .actorEmail(event.getActorEmail())
                .actorRole(event.getActorRole())
                .action(event.getAction())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .details(event.getDetails())
                .occurredAt(event.getOccurredAt())
                .build();
    }

    private record ActorSnapshot(Long userId, String email, Role role) {
        private static ActorSnapshot system() {
            return new ActorSnapshot(null, SYSTEM_ACTOR, null);
        }
    }
}
