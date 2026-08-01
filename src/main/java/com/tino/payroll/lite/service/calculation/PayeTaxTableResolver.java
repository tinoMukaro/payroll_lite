package com.tino.payroll.lite.service.calculation;

import com.tino.payroll.lite.entity.PayeTaxBand;
import com.tino.payroll.lite.entity.PayeTaxTable;
import com.tino.payroll.lite.enums.CurrencyCode;
import com.tino.payroll.lite.exception.PayrollConfigurationException;
import com.tino.payroll.lite.repository.PayeTaxTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayeTaxTableResolver {

    private final PayeTaxTableRepository repository;

    @Transactional(readOnly = true)
    public PayeParameters resolve(CurrencyCode currency, LocalDate payrollDate) {
        if (currency == null) {
            throw new IllegalArgumentException("Payroll currency is required");
        }
        if (payrollDate == null) {
            throw new IllegalArgumentException("Payroll date is required");
        }

        List<PayeTaxTable> applicableTables = repository.findApplicableTables(currency, payrollDate);
        if (applicableTables.isEmpty()) {
            throw new PayrollConfigurationException(
                    "No active PAYE tax table exists for " + currency + " on " + payrollDate
            );
        }
        if (applicableTables.size() > 1) {
            String versions = applicableTables.stream()
                    .map(PayeTaxTable::getVersion)
                    .sorted()
                    .reduce((first, second) -> first + ", " + second)
                    .orElse("unknown");
            throw new PayrollConfigurationException(
                    "Overlapping PAYE tax tables exist for " + currency + " on " + payrollDate
                            + ": " + versions
            );
        }

        PayeTaxTable table = applicableTables.getFirst();
        List<PayeTaxBandParameters> bands = table.getBands().stream()
                .sorted(Comparator.comparing(PayeTaxBand::getLowerBound))
                .map(band -> new PayeTaxBandParameters(
                        band.getLowerBound(),
                        band.getUpperBound(),
                        band.getRate()
                ))
                .toList();

        return new PayeParameters(table.getVersion(), table.getAidsLevyRate(), bands);
    }
}
