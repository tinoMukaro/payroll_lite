package com.tino.payroll.lite.repository;

import com.tino.payroll.lite.entity.NssaRule;
import com.tino.payroll.lite.enums.CurrencyCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface NssaRuleRepository extends JpaRepository<NssaRule, Long> {

    List<NssaRule> findAllByOrderByEffectiveFromDesc();

    boolean existsByVersion(String version);

    boolean existsByVersionAndIdNot(String version, Long id);

    @Query("""
            select (count(rule) > 0) from NssaRule rule
            where rule.currency = :currency
              and rule.active = true
              and (:excludedId is null or rule.id <> :excludedId)
              and rule.effectiveFrom <= :rangeEnd
              and (rule.effectiveTo is null or rule.effectiveTo >= :rangeStart)
            """)
    boolean existsOverlappingActiveRule(
            @Param("currency") CurrencyCode currency,
            @Param("rangeStart") LocalDate rangeStart,
            @Param("rangeEnd") LocalDate rangeEnd,
            @Param("excludedId") Long excludedId
    );

    @Query("""
            select rule from NssaRule rule
            where rule.currency = :currency
              and rule.active = true
              and rule.effectiveFrom <= :payrollDate
              and (rule.effectiveTo is null or rule.effectiveTo >= :payrollDate)
            order by rule.effectiveFrom desc
            """)
    List<NssaRule> findApplicableRules(
            @Param("currency") CurrencyCode currency,
            @Param("payrollDate") LocalDate payrollDate
    );
}