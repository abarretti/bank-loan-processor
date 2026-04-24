package com.ab.bankloanprocessor.service;

import com.ab.bankloanprocessor.controller.request.CreateLoanRequest;
import com.ab.bankloanprocessor.domain.LoanEventType;
import com.ab.bankloanprocessor.domain.LoanStateMachine;
import com.ab.bankloanprocessor.entity.Loan;
import com.ab.bankloanprocessor.entity.LoanEvent;
import com.ab.bankloanprocessor.repository.LoanEventRepository;
import com.ab.bankloanprocessor.repository.LoanRepository;
import com.ab.bankloanprocessor.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final PartyRepository partyRepository;
    private final LoanEventRepository loanEventRepository;
    private final LoanStateMachine stateMachine;

    public Loan create(CreateLoanRequest request) {
        var debtor = partyRepository.findById(request.debtorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST , "Debtor ID doesn't exist"));
        var creditor = partyRepository.findById(request.creditorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST , "Creditor ID doesn't exist"));
        var loan = Loan.builder()
                .debtor(debtor)
                .creditor(creditor)
                .principalAmount(request.amount())
                .build();
        var savedLoan = loanRepository.save(loan);

        var event = LoanEvent.builder()
                .loan(savedLoan)
                .party(creditor)
                .type(LoanEventType.CREATE)
                .idempotencyKey(request.idempotencyKey())
                .build();
        loanEventRepository.save(event);

        return savedLoan;
    }

    public Loan disburse(UUID loanId, BigDecimal amount) {
        var loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loan ID doesn't exist"));
        if (amount != null && amount.compareTo(loan.getPrincipalAmount()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Disburse amount cannot be greater than loan principal");
        }

        var event = LoanEvent.builder()
                .loan(loan)
                .party(loan.getCreditor())
                .type(LoanEventType.DISBURSE)
                .amount(Optional.ofNullable(amount)
                        .orElseGet(loan::getPrincipalAmount))
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
        stateMachine.apply(loan, event);
        var savedLoan = loanRepository.save(loan);

        loanEventRepository.save(event);

        return savedLoan;
    }

    public Loan repay(UUID loanId, BigDecimal amount) {
        var loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loan ID doesn't exist"));
        if (amount != null && amount.compareTo(loan.getBalance()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Repay amount cannot be greater than loan balance");
        }

        var event = LoanEvent.builder()
                .loan(loan)
                .party(loan.getDebtor())
                .type(LoanEventType.REPAY)
                .amount(Optional.ofNullable(amount)
                        .orElseGet(loan::getBalance))
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
        stateMachine.apply(loan, event);
        var savedLoan = loanRepository.save(loan);

        loanEventRepository.save(event);

        return savedLoan;
    }

    public Loan close(UUID loanId) {
        var loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loan ID doesn't exist"));

        var event = LoanEvent.builder()
                .loan(loan)
                .party(loan.getCreditor())
                .type(LoanEventType.CLOSE)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
        stateMachine.apply(loan, event);

        var savedLoan = loanRepository.save(loan);
        loanEventRepository.save(event);
        return savedLoan;
    }
}
