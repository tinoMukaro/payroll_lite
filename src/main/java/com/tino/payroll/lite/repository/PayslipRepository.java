package com.tino.payroll.lite.repository;

import com.tino.payroll.lite.entity.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayslipRepository extends JpaRepository<Payslip, Long>{
    List<Payslip> findByPayrollRunIdOrderByIdAsc(Long payrollRunId);
}
