package com.tino.payroll.lite.service;

import com.tino.payroll.lite.dto.NssaRuleRequest;
import com.tino.payroll.lite.dto.NssaRuleResponse;
import com.tino.payroll.lite.entity.NssaRule;
import com.tino.payroll.lite.exception.NssaRuleNotFoundException;
import com.tino.payroll.lite.exception.PayrollConfigurationException;
import com.tino.payroll.lite.repository.NssaRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NssaRuleService {

    private final NssaRuleRepository repository;

    @Transactional(readOnly = true)
    public List<NssaRuleResponse> getAll() {
        return repository.findAllByOrderByEffectiveFromDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public NssaRuleResponse create(NssaRuleRequest request) {
        validate(request, null);
        NssaRule rule = new NssaRule();
        apply(rule, request);
        return toResponse(repository.save(rule));
    }

    @Transactional
    public NssaRuleResponse update(Long id, NssaRuleRequest request) {
        NssaRule rule = find(id);
        validate(request, id);
        apply(rule, request);
        return toResponse(repository.save(rule));
    }

    private void validate(NssaRuleRequest request, Long excludedId) {
        if (request.getEffectiveTo() != null
                && request.getEffectiveTo().isBefore(request.getEffectiveFrom())) {
            throw new PayrollConfigurationException("Effective-to date cannot be before effective-from date");
        }

        boolean duplicateVersion = excludedId == null
                ? repository.existsByVersion(request.getVersion().trim())
                : repository.existsByVersionAndIdNot(request.getVersion().trim(), excludedId);
        if (duplicateVersion) {
            throw new PayrollConfigurationException("NSSA rule version already exists: " + request.getVersion().trim());
        }

        if (Boolean.TRUE.equals(request.getActive())) {
            LocalDate rangeEnd = request.getEffectiveTo() == null ? LocalDate.of(9999, 12, 31) : request.getEffectiveTo();
            if (repository.existsOverlappingActiveRule(
                    request.getCurrency(), request.getEffectiveFrom(), rangeEnd, excludedId)) {
                throw new PayrollConfigurationException(
                        "An active NSSA rule already covers part of this " + request.getCurrency() + " date range"
                );
            }
        }
    }

    private NssaRule find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NssaRuleNotFoundException("NSSA rule not found with ID: " + id));
    }

    private void apply(NssaRule rule, NssaRuleRequest request) {
        rule.setVersion(request.getVersion().trim());
        rule.setCurrency(request.getCurrency());
        rule.setEffectiveFrom(request.getEffectiveFrom());
        rule.setEffectiveTo(request.getEffectiveTo());
        rule.setEmployeeRate(request.getEmployeeRate());
        rule.setEmployerRate(request.getEmployerRate());
        rule.setPensionableEarningsCeiling(request.getPensionableEarningsCeiling());
        rule.setActive(request.getActive());
    }

    private NssaRuleResponse toResponse(NssaRule rule) {
        return NssaRuleResponse.builder()
                .id(rule.getId())
                .version(rule.getVersion())
                .currency(rule.getCurrency())
                .effectiveFrom(rule.getEffectiveFrom())
                .effectiveTo(rule.getEffectiveTo())
                .employeeRate(rule.getEmployeeRate())
                .employerRate(rule.getEmployerRate())
                .pensionableEarningsCeiling(rule.getPensionableEarningsCeiling())
                .active(rule.isActive())
                .build();
    }
}