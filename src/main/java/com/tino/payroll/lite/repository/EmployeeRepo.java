package com.tino.payroll.lite.repository;

import com.tino.payroll.lite.entity.Employee;
import com.tino.payroll.lite.enums.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRepo  extends JpaRepository<Employee, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmployeeNumber(String employeeNumber);

    List<Employee> findAllByStatus(EmployeeStatus status);

}
