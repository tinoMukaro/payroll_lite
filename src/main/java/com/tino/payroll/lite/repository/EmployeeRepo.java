package com.tino.payroll.lite.repository;

import com.tino.payroll.lite.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepo  extends JpaRepository<Employee, Long> {

}
