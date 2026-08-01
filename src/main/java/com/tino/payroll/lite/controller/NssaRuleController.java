package com.tino.payroll.lite.controller;

import com.tino.payroll.lite.dto.NssaRuleRequest;
import com.tino.payroll.lite.dto.NssaRuleResponse;
import com.tino.payroll.lite.service.NssaRuleService;
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
@Tag(name = "NSSA rules", description = "Effective-dated NSSA statutory configuration")
@RequestMapping("/api/nssa-rules")
@RequiredArgsConstructor
public class NssaRuleController {

    private final NssaRuleService service;
   // -----------------------------------------------------
    // GET ALL NSSA STATUTORY RULES
    // ----------------------------------------------------
    @GetMapping
    public ResponseEntity<List<NssaRuleResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }
   // -----------------------------------------------------
    // ADD NEW NSSA RULE
    // ----------------------------------------------------
    @PostMapping
    public ResponseEntity<NssaRuleResponse> create(@Valid @RequestBody NssaRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }
   // -----------------------------------------------------
    // EDIT/UPDATE AN EXISTING NSSA RULE
    // ----------------------------------------------------
    @PutMapping("/{id}")
    public ResponseEntity<NssaRuleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody NssaRuleRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }
}