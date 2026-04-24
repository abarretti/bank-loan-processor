package com.ab.bankloanprocessor.domain;

import com.ab.bankloanprocessor.entity.Loan;
import com.ab.bankloanprocessor.entity.LoanEvent;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Component
public class LoanStateMachine {

    public void apply(Loan loan, LoanEvent event) {
        switch (loan.getState()) {
            case CREATED -> handleCreated(loan, event);
            case DISBURSED -> handleDisbursed(loan, event);
            case REPAID -> handleRepaid(loan, event);
            case CLOSED -> reject(loan, event);
        }
    }

    private void handleCreated(Loan loan, LoanEvent event) {
        switch (event.getType()) {
            case ADD -> loan.addCollateral(event.getCollateral());
            case RESERVE -> loan.reserveCollateral(event.getCollateral());
            case PLEDGE -> loan.pledgeCollateral(event.getCollateral());
            case DISBURSE -> {
                loan.disbursePayment(event);
                loan.setState(LoanState.DISBURSED);
            }
            default -> reject(loan, event);
        }
    }

    private void handleDisbursed(Loan loan, LoanEvent event) {
        switch (event.getType()) {
            case REPAY -> {
                loan.repay(event);
                if (loan.getBalance().compareTo(BigDecimal.ZERO) == 0) {
                    loan.setState(LoanState.REPAID);
                }
            }
            default -> reject(loan, event);
        }
    }

    private void handleRepaid(Loan loan, LoanEvent event) {
        switch (event.getType()) {
            case RELEASE -> loan.releaseCollateral(event.getCollateral());
            case CLOSE -> {
                loan.close(event);
                loan.setState(LoanState.CLOSED);
            }
            default -> reject(loan, event);
        }
    }

    private void reject(Loan loan, LoanEvent event) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Event " + event.getType() + " not allowed in state " + loan.getState()
        );
    }
}
