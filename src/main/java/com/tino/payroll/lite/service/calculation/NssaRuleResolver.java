package com.tino.payroll.lite.service.calculation;

import com.tino.payroll.lite.entity.NssaRule;
import com.tino.payroll.lite.enums.CurrencyCode;
import com.tino.payroll.lite.exception.PayrollConfigurationException;
import com.tino.payroll.lite.repository.NssaRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NssaRuleResolver {

    private final NssaRuleRepository nssaRuleRepository;

    @Transactional(readOnly = true)
    public NssaParameters resolve(CurrencyCode currency, LocalDate payrollDate) {
        if (currency == null) {
            throw new IllegalArgumentException("Payroll currency is required");
        }
        if (payrollDate == null) {
            throw new IllegalArgumentException("Payroll date is required");
        }

        List<NssaRule> applicableRules =
                nssaRuleRepository.findApplicableRules(currency, payrollDate);

        if (applicableRules.isEmpty()) {
            throw new PayrollConfigurationException(
                    "No active NSSA rule exists for " + currency + " on " + payrollDate
            );
        }
        if (applicableRules.size() > 1) {
            String versions = applicableRules.stream()
                    .map(NssaRule::getVersion)
                    .sorted()
                    .reduce((first, second) -> first + ", " + second)
                    .orElse("unknown");
            throw new PayrollConfigurationException(
                    "Overlapping NSSA rules exist for " + currency + " on " + payrollDate
                            + ": " + versions
            );
        }

        NssaRule rule = applicableRules.getFirst();
        return new NssaParameters(
                rule.getEmployeeRate(),
                rule.getEmployerRate(),
                rule.getPensionableEarningsCeiling(),
                rule.getVersion()
        );
    }
}