package com.ab.bankloanprocessor.service;

import com.ab.bankloanprocessor.controller.request.AddCollateralRequest;
import com.ab.bankloanprocessor.domain.CollateralStatus;
import com.ab.bankloanprocessor.domain.LoanEventType;
import com.ab.bankloanprocessor.domain.LoanStateMachine;
import com.ab.bankloanprocessor.entity.Collateral;
import com.ab.bankloanprocessor.entity.LoanEvent;
import com.ab.bankloanprocessor.repository.CollateralRepository;
import com.ab.bankloanprocessor.repository.LoanEventRepository;
import com.ab.bankloanprocessor.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CollateralService {

    private final LoanRepository loanRepository;
    private final LoanEventRepository loanEventRepository;
    private final CollateralRepository collateralRepository;
    private final LoanStateMachine stateMachine;

    public List<Map<UUID, CollateralStatus>> add(UUID loanId, AddCollateralRequest request) {
        var loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loan ID doesn't exist"));
        var event = LoanEvent.builder()
                .loan(loan)
                .party(loan.getDebtor())
                .type(LoanEventType.ADD)
                .idempotencyKey(UUID.randomUUID().toString())
                .collateral(Collateral.builder()
                        .loan(loan)
                        .metadata(request.metadata())
                        .build())
                .build();
        stateMachine.apply(loan, event);
        var savedLoan = loanRepository.save(loan);

        loanEventRepository.save(event);
        return savedLoan.getCollaterals().stream()
                .map(c -> Map.of(c.getCollateralId(), c.getStatus()))
                .toList();
    }

    public List<Map<UUID, CollateralStatus>> reserve(UUID collateralId) {
        var collateral = collateralRepository.findById(collateralId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Collateral ID doesn't exist"));
        var loan = collateral.getLoan();
        var event = LoanEvent.builder()
                .loan(loan)
                .party(loan.getDebtor())
                .type(LoanEventType.RESERVE)
                .idempotencyKey(UUID.randomUUID().toString())
                .collateral(Collateral.builder()
                        .collateralId(collateralId)
                        .loan(loan)
                        .status(CollateralStatus.RESERVED)
                        .metadata(collateral.getMetadata())
                        .build())
                .build();
        stateMachine.apply(loan, event);
        var savedLoan = loanRepository.save(loan);

        loanEventRepository.save(event);
        return savedLoan.getCollaterals().stream()
                .map(c -> Map.of(c.getCollateralId(), c.getStatus()))
                .toList();
    }

    public List<Map<UUID, CollateralStatus>> pledge(UUID collateralId) {
        var collateral = collateralRepository.findById(collateralId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Collateral ID doesn't exist"));
        var loan = collateral.getLoan();
        var event = LoanEvent.builder()
                .loan(loan)
                .party(loan.getDebtor())
                .type(LoanEventType.PLEDGE)
                .idempotencyKey(UUID.randomUUID().toString())
                .collateral(Collateral.builder()
                        .collateralId(collateralId)
                        .loan(loan)
                        .status(CollateralStatus.PLEDGED)
                        .metadata(collateral.getMetadata())
                        .build())
                .build();
        stateMachine.apply(loan, event);
        var savedLoan = loanRepository.save(loan);

        loanEventRepository.save(event);
        var collateralIds = savedLoan.getCollaterals().stream()
                .map(c -> Map.of(c.getCollateralId(), c.getStatus()))
                .toList();

        return collateralIds;
    }

    public List<Map<UUID, CollateralStatus>> release(UUID collateralId) {
        var collateral = collateralRepository.findById(collateralId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Collateral ID doesn't exist"));
        var loan = collateral.getLoan();
        var event = LoanEvent.builder()
                .loan(loan)
                .party(loan.getDebtor())
                .type(LoanEventType.RELEASE)
                .idempotencyKey(UUID.randomUUID().toString())
                .collateral(Collateral.builder()
                        .collateralId(collateralId)
                        .loan(loan)
                        .status(CollateralStatus.RELEASED)
                        .metadata(collateral.getMetadata())
                        .build())
                .build();
        stateMachine.apply(loan, event);

        var savedLoan = loanRepository.save(loan);
        loanEventRepository.save(event);
        return savedLoan.getCollaterals().stream()
                .map(c -> Map.of(c.getCollateralId(), c.getStatus()))
                .toList();
    }
}
