package com.tino.payroll.lite.repository;

import com.tino.payroll.lite.entity.Employee;
import com.tino.payroll.lite.enums.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepo extends JpaRepository<Employee, Long> {
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    Optional<Employee> findByEmailIgnoreCase(String email);
    Optional<Employee> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    List<Employee> findAllByStatus(EmployeeStatus status);
    List<Employee> findAllByUserIsNull();
}