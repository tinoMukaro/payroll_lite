package com.tino.payroll.lite.service;

import com.tino.payroll.lite.entity.Employee;
import com.tino.payroll.lite.entity.User;
import com.tino.payroll.lite.enums.EmployeeStatus;
import com.tino.payroll.lite.repository.EmployeeRepo;
import com.tino.payroll.lite.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountHarmonizationService {

    private final EmployeeRepo employeeRepo;
    private final UserRepository userRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void linkExistingAccounts() {
        for (Employee employee : employeeRepo.findAllByUserIsNull()) {
            User user = userRepository.findByEmailIgnoreCase(employee.getEmail()).orElse(null);
            if (user == null || employeeRepo.existsByUserId(user.getId())) continue;

            employee.setUser(user);
            if (employee.getStatus() == EmployeeStatus.TERMINATED) {
                user.setEnabled(false);
                userRepository.save(user);
            }
            employeeRepo.save(employee);
        }
    }
}