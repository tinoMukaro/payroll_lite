package com.tino.payroll.lite.repository;

import com.tino.payroll.lite.entity.RecurringPayItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringPayItemRepository extends JpaRepository<RecurringPayItem, Long> {

    @EntityGraph(attributePaths = "employee")
    List<RecurringPayItem> findByEmployeeIdOrderByActiveDescEffectiveFromDescIdDesc(Long employeeId);

    @EntityGraph(attributePaths = "employee")
    Optional<RecurringPayItem> findByIdAndEmployeeId(Long id, Long employeeId);

    @EntityGraph(attributePaths = "employee")
    @Query("""
            select item from RecurringPayItem item
            where item.employee.id in :employeeIds
              and item.active = true
              and item.effectiveFrom <= :payrollDate
              and (item.effectiveTo is null or item.effectiveTo >= :payrollDate)
            order by item.employee.id, item.id
            """)
    List<RecurringPayItem> findApplicable(
            @Param("employeeIds") List<Long> employeeIds,
            @Param("payrollDate") LocalDate payrollDate
    );
}
