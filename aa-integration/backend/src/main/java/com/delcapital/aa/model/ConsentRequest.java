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
@Table(name = "consent_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConsentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "digio_consent_id")
    private String digioConsentId;

    @Column(name = "template_id", nullable = false)
    private String templateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ConsentStatus status;

    @Column(name = "purpose_code", nullable = false)
    private String purposeCode;

    @Column(name = "purpose_text")
    private String purposeText;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "fi_types", columnDefinition = "text[]")
    private List<String> fiTypes;

    @Column(name = "date_range_from")
    private LocalDate dateRangeFrom;

    @Column(name = "date_range_to")
    private LocalDate dateRangeTo;

    @Column(name = "consent_expiry")
    private OffsetDateTime consentExpiry;

    @Column(name = "redirect_url")
    private String redirectUrl;

    @Column(name = "digio_redirect_url")
    private String digioRedirectUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_request", columnDefinition = "jsonb")
    private Map<String, Object> rawRequest;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response", columnDefinition = "jsonb")
    private Map<String, Object> rawResponse;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (status == null) status = ConsentStatus.PENDING;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum ConsentStatus {
        PENDING, ACTIVE, PAUSED, REVOKED, EXPIRED, FAILED
    }
}
