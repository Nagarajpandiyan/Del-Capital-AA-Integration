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
@Table(name = "financial_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FinancialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "fetch_id", nullable = false)
    private UUID fetchId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "fip_id")
    private String fipId;

    @Column(name = "account_type")
    private String accountType;

    @Column(name = "masked_account")
    private String maskedAccount;

    @Column(name = "ifsc_code")
    private String ifscCode;

    @Column(name = "currency")
    private String currency;

    @Column(name = "balance")
    private BigDecimal balance;

    @Column(name = "balance_date")
    private LocalDate balanceDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    private Map<String, Object> rawData;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = OffsetDateTime.now();
        if (currency == null) currency = "INR";
    }
}
