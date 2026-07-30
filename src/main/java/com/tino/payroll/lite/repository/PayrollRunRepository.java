package com.tino.payroll.lite.repository;


import com.tino.payroll.lite.entity.PayrollRun;
import com.tino.payroll.lite.enums.CurrencyCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {
    boolean existsByMonthAndYearAndCurrency(Integer month, Integer year, CurrencyCode currency);
}
