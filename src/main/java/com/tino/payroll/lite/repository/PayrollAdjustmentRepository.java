package com.tino.payroll.lite.repository;

import com.tino.payroll.lite.entity.PayrollAdjustment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayrollAdjustmentRepository extends JpaRepository<PayrollAdjustment, Long> {

    @EntityGraph(attributePaths = {"employee", "payrollRun"})
    List<PayrollAdjustment> findByPayrollRunIdOrderByIdAsc(Long payrollRunId);

    @EntityGraph(attributePaths = {"employee", "payrollRun"})
    Optional<PayrollAdjustment> findByIdAndPayrollRunId(Long id, Long payrollRunId);
}
