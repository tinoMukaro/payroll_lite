package com.tino.payroll.lite.controller;

import com.tino.payroll.lite.dto.PayeTaxTableRequest;
import com.tino.payroll.lite.dto.PayeTaxTableResponse;
import com.tino.payroll.lite.service.PayeTaxTableService;
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
@Tag(name = "PAYE tax tables", description = "Effective-dated progressive PAYE configuration")
@RequestMapping("/api/paye-tax-tables")
@RequiredArgsConstructor
public class PayeTaxTableController {

    private final PayeTaxTableService service;

    @GetMapping
    public ResponseEntity<List<PayeTaxTableResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PostMapping
    public ResponseEntity<PayeTaxTableResponse> create(
            @Valid @RequestBody PayeTaxTableRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PayeTaxTableResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PayeTaxTableRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }
}
