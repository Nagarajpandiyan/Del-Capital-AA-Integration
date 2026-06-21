package com.delcapital.aa.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "fetch_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FetchRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consent_id", nullable = false)
    private ConsentRequest consentRequest;

    @Column(name = "digio_fetch_id")
    private String digioFetchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FetchStatus status;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "fi_types", columnDefinition = "text[]")
    private List<String> fiTypes;

    @Column(name = "initiated_by")
    private String initiatedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_request", columnDefinition = "jsonb")
    private Map<String, Object> rawRequest;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response", columnDefinition = "jsonb")
    private Map<String, Object> rawResponse;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "retry_count")
    private int retryCount;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (status == null) status = FetchStatus.INITIATED;
        if (retryCount == 0) retryCount = 0;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum FetchStatus {
        INITIATED, PENDING, PARTIAL, COMPLETED, FAILED, TIMEOUT
    }
}
