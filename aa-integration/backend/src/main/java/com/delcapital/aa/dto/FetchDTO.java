package com.delcapital.aa.dto;

import com.delcapital.aa.model.FetchRequest;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class FetchDTO {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class InitiateRequest {
        @NotNull(message = "Consent ID is required")
        private UUID consentId;

        @NotNull(message = "From date is required")
        private LocalDate fromDate;

        @NotNull(message = "To date is required")
        private LocalDate toDate;

        private List<String> fiTypes;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private UUID fetchId;
        private UUID consentId;
        private String digioFetchId;
        private String status;
        private LocalDate fromDate;
        private LocalDate toDate;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public static Response from(FetchRequest fr) {
            return Response.builder()
                    .fetchId(fr.getId())
                    .consentId(fr.getConsentRequest().getId())
                    .digioFetchId(fr.getDigioFetchId())
                    .status(fr.getStatus().name())
                    .fromDate(fr.getFromDate())
                    .toDate(fr.getToDate())
                    .createdAt(fr.getCreatedAt())
                    .updatedAt(fr.getUpdatedAt())
                    .build();
        }
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DataResponse {
        private UUID fetchId;
        private String status;
        private List<AccountDTO> accounts;
        private List<LoanDTO> loans;

        @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
        public static class AccountDTO {
            private UUID accountId;
            private String fipId;
            private String accountType;
            private String maskedAccount;
            private String ifscCode;
            private String currency;
            private Double balance;
            private LocalDate balanceDate;
            private List<TransactionDTO> transactions;
        }

        @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
        public static class TransactionDTO {
            private UUID transactionId;
            private LocalDate transactionDate;
            private Double amount;
            private String type;
            private String narration;
            private String reference;
            private String mode;
        }

        @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
        public static class LoanDTO {
            private UUID loanId;
            private String fipId;
            private String loanType;
            private String maskedAccount;
            private Double principal;
            private Double outstanding;
            private Double emiAmount;
            private Double interestRate;
            private Integer tenureMonths;
        }
    }
}
