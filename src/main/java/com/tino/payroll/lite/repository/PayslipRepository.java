package com.tino.payroll.lite.repository;

import com.tino.payroll.lite.entity.Payslip;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayslipRepository extends JpaRepository<Payslip, Long> {
    @EntityGraph(attributePaths = {"employee", "employee.user", "payrollRun", "lineItems"})
    @Query("select payslip from Payslip payslip where payslip.id = :id")
    Optional<Payslip> findForDownload(@Param("id") Long id);

    @EntityGraph(attributePaths = {"employee", "payrollRun", "lineItems"})
    List<Payslip> findByPayrollRunIdOrderByIdAsc(Long payrollRunId);

    @Query("""
            select distinct payslip from Payslip payslip
            join fetch payslip.employee employee
            join fetch payslip.payrollRun payrollRun
            left join fetch payslip.lineItems lineItems
            where employee.user.id = :userId
            order by payrollRun.year desc, payrollRun.month desc
            """)
    List<Payslip> findForUser(@Param("userId") Long userId);
}
