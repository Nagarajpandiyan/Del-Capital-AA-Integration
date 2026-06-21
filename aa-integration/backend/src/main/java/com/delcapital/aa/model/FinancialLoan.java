package com.delcapital.aa.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "financial_loans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FinancialLoan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "fetch_id", nullable = false)
    private UUID fetchId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "fip_id")
    private String fipId;

    @Column(name = "loan_type")
    private String loanType;

    @Column(name = "masked_account")
    private String maskedAccount;

    @Column(name = "principal")
    private BigDecimal principal;

    @Column(name = "outstanding")
    private BigDecimal outstanding;

    @Column(name = "emi_amount")
    private BigDecimal emiAmount;

    @Column(name = "interest_rate")
    private BigDecimal interestRate;

    @Column(name = "tenure_months")
    private Integer tenureMonths;

    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    private Map<String, Object> rawData;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = OffsetDateTime.now();
    }
}
