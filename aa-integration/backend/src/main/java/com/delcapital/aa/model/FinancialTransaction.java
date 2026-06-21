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
@Table(name = "financial_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FinancialTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "fetch_id", nullable = false)
    private UUID fetchId;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "tx_type")
    private String txType;

    @Column(name = "narration")
    private String narration;

    @Column(name = "reference")
    private String reference;

    @Column(name = "mode")
    private String mode;

    @Column(name = "category")
    private String category;

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
