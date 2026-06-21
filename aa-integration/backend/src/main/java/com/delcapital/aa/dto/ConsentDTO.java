package com.delcapital.aa.dto;

import com.delcapital.aa.model.ConsentRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class ConsentDTO {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateRequest {
        @NotBlank(message = "Mobile number is required")
        private String mobileNumber;

        private String aaHandle;
        private String email;

        @NotBlank(message = "Purpose code is required")
        private String purposeCode;

        private String purposeText;

        @NotEmpty(message = "At least one FI type is required")
        private List<String> fiTypes;

        private LocalDate dateRangeFrom;
        private LocalDate dateRangeTo;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private UUID consentId;
        private String digioConsentId;
        private String status;
        private String redirectUrl;
        private OffsetDateTime createdAt;
        private OffsetDateTime expiresAt;

        public static Response from(ConsentRequest cr) {
            return Response.builder()
                    .consentId(cr.getId())
                    .digioConsentId(cr.getDigioConsentId())
                    .status(cr.getStatus().name())
                    .redirectUrl(cr.getDigioRedirectUrl())
                    .createdAt(cr.getCreatedAt())
                    .expiresAt(cr.getConsentExpiry())
                    .build();
        }
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StatusResponse {
        private UUID consentId;
        private String digioConsentId;
        private String status;
        private String purposeCode;
        private List<String> fiTypes;
        private LocalDate dateRangeFrom;
        private LocalDate dateRangeTo;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private OffsetDateTime expiresAt;

        public static StatusResponse from(ConsentRequest cr) {
            return StatusResponse.builder()
                    .consentId(cr.getId())
                    .digioConsentId(cr.getDigioConsentId())
                    .status(cr.getStatus().name())
                    .purposeCode(cr.getPurposeCode())
                    .fiTypes(cr.getFiTypes())
                    .dateRangeFrom(cr.getDateRangeFrom())
                    .dateRangeTo(cr.getDateRangeTo())
                    .createdAt(cr.getCreatedAt())
                    .updatedAt(cr.getUpdatedAt())
                    .expiresAt(cr.getConsentExpiry())
                    .build();
        }
    }
}
