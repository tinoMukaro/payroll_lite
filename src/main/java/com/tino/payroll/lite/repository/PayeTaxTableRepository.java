package com.tino.payroll.lite.repository;

import com.tino.payroll.lite.entity.PayeTaxTable;
import com.tino.payroll.lite.enums.CurrencyCode;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PayeTaxTableRepository extends JpaRepository<PayeTaxTable, Long> {

    @EntityGraph(attributePaths = "bands")
    List<PayeTaxTable> findAllByOrderByEffectiveFromDesc();

    boolean existsByVersion(String version);

    boolean existsByVersionAndIdNot(String version, Long id);

    @Query("""
            select (count(taxTable) > 0) from PayeTaxTable taxTable
            where taxTable.currency = :currency
              and taxTable.active = true
              and (:excludedId is null or taxTable.id <> :excludedId)
              and taxTable.effectiveFrom <= :rangeEnd
              and (taxTable.effectiveTo is null or taxTable.effectiveTo >= :rangeStart)
            """)
    boolean existsOverlappingActiveTable(
            @Param("currency") CurrencyCode currency,
            @Param("rangeStart") LocalDate rangeStart,
            @Param("rangeEnd") LocalDate rangeEnd,
            @Param("excludedId") Long excludedId
    );

    @EntityGraph(attributePaths = "bands")
    @Query("""
            select taxTable from PayeTaxTable taxTable
            where taxTable.currency = :currency
              and taxTable.active = true
              and taxTable.effectiveFrom <= :payrollDate
              and (taxTable.effectiveTo is null or taxTable.effectiveTo >= :payrollDate)
            order by taxTable.effectiveFrom desc
            """)
    List<PayeTaxTable> findApplicableTables(
            @Param("currency") CurrencyCode currency,
            @Param("payrollDate") LocalDate payrollDate
    );
}
