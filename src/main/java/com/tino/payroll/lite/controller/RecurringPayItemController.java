package com.tino.payroll.lite.controller;

import com.tino.payroll.lite.dto.RecurringPayItemRequest;
import com.tino.payroll.lite.dto.RecurringPayItemResponse;
import com.tino.payroll.lite.service.RecurringPayItemService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Recurring pay items", description = "Fixed recurring earnings and deductions assigned to employees")
@RequestMapping("/api/employees/{employeeId}/recurring-pay-items")
@RequiredArgsConstructor
public class RecurringPayItemController {

    private final RecurringPayItemService payItemService;

    @GetMapping
    public ResponseEntity<List<RecurringPayItemResponse>> list(@PathVariable Long employeeId) {
        return ResponseEntity.ok(payItemService.list(employeeId));
    }

    @PostMapping
    public ResponseEntity<RecurringPayItemResponse> create(
            @PathVariable Long employeeId,
            @Valid @RequestBody RecurringPayItemRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(payItemService.create(employeeId, request));
    }

    @PutMapping("/{payItemId}")
    public ResponseEntity<RecurringPayItemResponse> update(
            @PathVariable Long employeeId,
            @PathVariable Long payItemId,
            @Valid @RequestBody RecurringPayItemRequest request
    ) {
        return ResponseEntity.ok(payItemService.update(employeeId, payItemId, request));
    }
}
