package com.ab.bankloanprocessor.controller;

import com.ab.bankloanprocessor.controller.request.CreateLoanRequest;
import com.ab.bankloanprocessor.entity.Loan;
import com.ab.bankloanprocessor.service.LoanService;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/loan")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService service;

    @PostMapping
    public ResponseEntity<Loan> createLoan(@Valid @RequestBody CreateLoanRequest request) {
        var loan = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(loan);
    }

    @PatchMapping("/disburse/{loan_id}")
    public ResponseEntity<Loan> disburse(@PathVariable(name = "loan_id") UUID loanId,
                                         @RequestParam(name = "amount") @Nullable BigDecimal amount) {
        var loan = service.disburse(loanId, amount);
        return ResponseEntity.status(HttpStatus.OK)
                .body(loan);
    }

    @PatchMapping("/repay/{loan_id}")
    public ResponseEntity<Loan> repay(@PathVariable(name = "loan_id") UUID loanId,
                                      @RequestParam(name = "amount") @Nullable BigDecimal amount) {
        var loan = service.repay(loanId, amount);
        return ResponseEntity.status(HttpStatus.OK)
                .body(loan);
    }

    @PatchMapping("/close/{loan_id}")
    public ResponseEntity<Loan> close(@PathVariable(name = "loan_id") UUID loanId) {
        var loan = service.close(loanId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(loan);
    }
}
