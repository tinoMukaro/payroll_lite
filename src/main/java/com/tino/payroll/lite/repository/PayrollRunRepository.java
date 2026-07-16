package com.tino.payroll.lite.repository;


import com.tino.payroll.lite.entity.PayrollRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {
    boolean existsByMonthAndYear(Integer month, Integer year);
}
