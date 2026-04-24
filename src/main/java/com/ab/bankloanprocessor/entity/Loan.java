package com.ab.bankloanprocessor.entity;

import com.ab.bankloanprocessor.domain.CollateralStatus;
import com.ab.bankloanprocessor.domain.LoanState;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "loan")
@Getter
@Setter
public class Loan extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "debtor_party_id", nullable = false)
    private Party debtor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creditor_party_id", nullable = false)
    private Party creditor;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "state", nullable = false, columnDefinition = "loan_state")
    private LoanState state = LoanState.CREATED;

    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalAmount;

    @Builder.Default
    @Column(name = "amount_disbursed", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountDisbursed = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "amount_paid", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Builder.Default
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Collateral> collaterals = new ArrayList<>();

    @Transient
    public BigDecimal getBalance() {
        return amountDisbursed.subtract(amountPaid);
    }

    public void addCollateral(Collateral collateral) {
        getCollaterals().add(collateral);
    }

    public void reserveCollateral(Collateral collateral) {
        collaterals.stream()
                .filter(c -> c.getCollateralId().equals(collateral.getCollateralId())
                    && c.getStatus().equals(CollateralStatus.NONE))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Collateral ID can only be RESERVED if status is NONE"))
                .setStatus(CollateralStatus.RESERVED);
    }

    public void pledgeCollateral(Collateral collateral) {
        collaterals.stream()
                .filter(c -> c.getCollateralId().equals(collateral.getCollateralId())
                        && c.getStatus().equals(CollateralStatus.RESERVED))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Collateral ID can only be PLEDGED if status is RESERVED"))
                .setStatus(CollateralStatus.PLEDGED);
    }

    public void releaseCollateral(Collateral collateral) {
        collaterals.stream()
                .filter(c -> c.getCollateralId().equals(collateral.getCollateralId())
                        && c.getStatus().equals(CollateralStatus.PLEDGED))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Collateral ID can only be RELEASED if status is PLEDGED"))
                .setStatus(CollateralStatus.RELEASED);
    }

    public void disbursePayment(LoanEvent event) {
        if (collaterals.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Collateral must be ADDED before payment can be DISBURSED");
        }
        for (Collateral collateral : collaterals) {
            if (!collateral.getStatus().equals(CollateralStatus.PLEDGED)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "All Collateral must be PLEDGED before payment can be DISBURSED");
            }
        }
        amountDisbursed = amountDisbursed.add(event.getAmount());
    }

    public void repay(LoanEvent event) {
        amountPaid = amountPaid.add(event.getAmount());
    }

    public void close(LoanEvent event) {
        for (Collateral collateral : collaterals) {
            if (!collateral.getStatus().equals(CollateralStatus.RELEASED)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "All Collateral must be RELEASED before loan can be CLOSED");
            }
        }
    }
}
