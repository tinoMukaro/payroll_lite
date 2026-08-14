package com.tino.payroll.lite.service;

import com.tino.payroll.lite.dto.PayeTaxBandRequest;
import com.tino.payroll.lite.dto.PayeTaxBandResponse;
import com.tino.payroll.lite.dto.PayeTaxTableRequest;
import com.tino.payroll.lite.dto.PayeTaxTableResponse;
import com.tino.payroll.lite.entity.PayeTaxBand;
import com.tino.payroll.lite.entity.PayeTaxTable;
import com.tino.payroll.lite.enums.AuditAction;
import com.tino.payroll.lite.enums.AuditEntityType;
import com.tino.payroll.lite.exception.PayeTaxTableNotFoundException;
import com.tino.payroll.lite.exception.PayrollConfigurationException;
import com.tino.payroll.lite.repository.PayeTaxTableRepository;
import com.tino.payroll.lite.service.calculation.PayeCalculator;
import com.tino.payroll.lite.service.calculation.PayeParameters;
import com.tino.payroll.lite.service.calculation.PayeTaxBandParameters;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayeTaxTableService {

    private final PayeTaxTableRepository repository;
    private final PayeCalculator calculator;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<PayeTaxTableResponse> getAll() {
        return repository.findAllByOrderByEffectiveFromDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PayeTaxTableResponse create(PayeTaxTableRequest request) {
        validate(request, null);
        PayeTaxTable table = new PayeTaxTable();
        apply(table, request);
        PayeTaxTable savedTable = repository.save(table);
        auditService.record(
                AuditAction.PAYE_TABLE_CREATED,
                AuditEntityType.PAYE_TABLE,
                savedTable.getId(),
                "Created PAYE table " + savedTable.getVersion()
        );
        return toResponse(savedTable);
    }

    @Transactional
    public PayeTaxTableResponse update(Long id, PayeTaxTableRequest request) {
        PayeTaxTable table = find(id);
        validate(request, id);
        apply(table, request);
        PayeTaxTable savedTable = repository.save(table);
        auditService.record(
                AuditAction.PAYE_TABLE_UPDATED,
                AuditEntityType.PAYE_TABLE,
                savedTable.getId(),
                "Updated PAYE table " + savedTable.getVersion()
        );
        return toResponse(savedTable);
    }

    private void validate(PayeTaxTableRequest request, Long excludedId) {
        if (request.getEffectiveTo() != null
                && request.getEffectiveTo().isBefore(request.getEffectiveFrom())) {
            throw new PayrollConfigurationException(
                    "Effective-to date cannot be before effective-from date"
            );
        }

        String version = request.getVersion().trim();
        boolean duplicateVersion = excludedId == null
                ? repository.existsByVersion(version)
                : repository.existsByVersionAndIdNot(version, excludedId);
        if (duplicateVersion) {
            throw new PayrollConfigurationException("PAYE table version already exists: " + version);
        }

        validateBands(request);

        if (Boolean.TRUE.equals(request.getActive())) {
            LocalDate rangeEnd = request.getEffectiveTo() == null
                    ? LocalDate.of(9999, 12, 31)
                    : request.getEffectiveTo();
            if (repository.existsOverlappingActiveTable(
                    request.getCurrency(), request.getEffectiveFrom(), rangeEnd, excludedId)) {
                throw new PayrollConfigurationException(
                        "An active PAYE table already covers part of this "
                                + request.getCurrency() + " date range"
                );
            }
        }
    }

    private void validateBands(PayeTaxTableRequest request) {
        List<PayeTaxBandParameters> bands = request.getBands().stream()
                .map(this::toParameters)
                .toList();
        calculator.calculate(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new PayeParameters(
                        request.getVersion().trim(),
                        request.getAidsLevyRate(),
                        bands
                )
        );
    }

    private PayeTaxBandParameters toParameters(PayeTaxBandRequest band) {
        return new PayeTaxBandParameters(
                band.getLowerBound(),
                band.getUpperBound(),
                band.getRate()
        );
    }

    private PayeTaxTable find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new PayeTaxTableNotFoundException(
                        "PAYE tax table not found with ID: " + id
                ));
    }

    private void apply(PayeTaxTable table, PayeTaxTableRequest request) {
        table.setVersion(request.getVersion().trim());
        table.setCurrency(request.getCurrency());
        table.setEffectiveFrom(request.getEffectiveFrom());
        table.setEffectiveTo(request.getEffectiveTo());
        table.setAidsLevyRate(request.getAidsLevyRate());
        table.setActive(request.getActive());

        table.getBands().clear();
        request.getBands().stream()
                .sorted(Comparator.comparing(PayeTaxBandRequest::getLowerBound))
                .map(requestBand -> PayeTaxBand.builder()
                        .taxTable(table)
                        .lowerBound(requestBand.getLowerBound())
                        .upperBound(requestBand.getUpperBound())
                        .rate(requestBand.getRate())
                        .build())
                .forEach(table.getBands()::add);
    }

    private PayeTaxTableResponse toResponse(PayeTaxTable table) {
        List<PayeTaxBandResponse> bands = table.getBands().stream()
                .sorted(Comparator.comparing(PayeTaxBand::getLowerBound))
                .map(band -> PayeTaxBandResponse.builder()
                        .id(band.getId())
                        .lowerBound(band.getLowerBound())
                        .upperBound(band.getUpperBound())
                        .rate(band.getRate())
                        .build())
                .toList();

        return PayeTaxTableResponse.builder()
                .id(table.getId())
                .version(table.getVersion())
                .currency(table.getCurrency())
                .effectiveFrom(table.getEffectiveFrom())
                .effectiveTo(table.getEffectiveTo())
                .aidsLevyRate(table.getAidsLevyRate())
                .active(table.isActive())
                .bands(bands)
                .build();
    }
}
