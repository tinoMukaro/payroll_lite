package com.tino.payroll.lite.repository;

import com.tino.payroll.lite.entity.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayslipRepository extends JpaRepository<Payslip, Long> {
    List<Payslip> findByPayrollRunIdOrderByIdAsc(Long payrollRunId);

    @Query("""
            select payslip from Payslip payslip
            join fetch payslip.employee employee
            join fetch payslip.payrollRun payrollRun
            where employee.user.id = :userId
            order by payrollRun.year desc, payrollRun.month desc
            """)
    List<Payslip> findForUser(@Param("userId") Long userId);
}