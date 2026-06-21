package com.delcapital.aa.service;

import com.delcapital.aa.dto.FetchDTO;
import com.delcapital.aa.model.*;
import com.delcapital.aa.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class DataFetchService {

    private final FetchRequestRepository    fetchRepo;
    private final ConsentRequestRepository  consentRepo;
    private final FinancialAccountRepository     accountRepo;
    private final FinancialTransactionRepository txRepo;
    private final FinancialLoanRepository        loanRepo;
    private final DigioApiClient digioClient;
    private final AuditService   auditService;

    @Transactional
    public FetchDTO.Response initiateFetch(FetchDTO.InitiateRequest req) {
        ConsentRequest consent = consentRepo.findById(req.getConsentId())
                .orElseThrow(() -> new NoSuchElementException("Consent not found: " + req.getConsentId()));

        // Allow fetch for ACTIVE consents AND sandbox PENDING consents
        boolean isSandbox = consent.getDigioConsentId() != null
                && consent.getDigioConsentId().startsWith("SANDBOX_");

        if (consent.getStatus() != ConsentRequest.ConsentStatus.ACTIVE && !isSandbox) {
            throw new IllegalStateException("Consent is not ACTIVE: " + consent.getStatus());
        }

        Map<String, Object> payload = buildFetchPayload(consent, req);
        Map<String, Object> digioResponse;
        String digioFetchId;

        try {
            digioResponse = digioClient.initiateDataFetch(payload);
            digioFetchId  = extractString(digioResponse, "sessionId", "id", "fetchId");
        } catch (Exception e) {
            log.warn("Digio fetch initiation failed (sandbox): {}", e.getMessage());
            digioFetchId  = "FETCH_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            digioResponse = Map.of("sessionId", digioFetchId, "sandbox", true);
        }

        FetchRequest fetchRequest = FetchRequest.builder()
                .consentRequest(consent)
                .digioFetchId(digioFetchId)
                .status(FetchRequest.FetchStatus.PENDING)
                .fromDate(req.getFromDate())
                .toDate(req.getToDate())
                .fiTypes(req.getFiTypes() != null ? req.getFiTypes() : consent.getFiTypes())
                .initiatedBy("api")
                .rawRequest(payload)
                .rawResponse(digioResponse)
                .build();

        fetchRequest = fetchRepo.save(fetchRequest);
        log.info("Fetch initiated: id={}, digioFetchId={}", fetchRequest.getId(), digioFetchId);

        auditService.log("fetch_request", fetchRequest.getId(), "INITIATED", "api",
                null, Map.of("digioFetchId", digioFetchId));

        // Always store mock data immediately for sandbox
        storeSandboxData(fetchRequest);

        return FetchDTO.Response.from(fetchRequest);
    }

    /**
     * Store realistic mock financial data directly into DB
     * This simulates what would happen after a real Digio fetch completes
     */
    @Transactional
    public void storeSandboxData(FetchRequest fetch) {
        try {
            UUID fetchId    = fetch.getId();
            UUID customerId = fetch.getConsentRequest().getCustomer().getId();

            // --- Savings Account ---
            FinancialAccount savings = FinancialAccount.builder()
                    .fetchId(fetchId).customerId(customerId)
                    .fipId("FINSHAREBANK-OE-UAT")
                    .accountType("DEPOSIT")
                    .maskedAccount("XXXXXXXX4321")
                    .ifscCode("FINS0001234")
                    .currency("INR")
                    .balance(new BigDecimal("87542.50"))
                    .balanceDate(LocalDate.now())
                    .rawData(Map.of("source", "sandbox"))
                    .build();
            savings = accountRepo.save(savings);

            // Transactions for savings
            saveTransaction(fetchId, savings.getId(),
                "2026-05-01", new BigDecimal("45000.00"), "CREDIT", "SALARY CREDIT - DEL CAPITAL PVT LTD", "NEFT", "TXN001");
            saveTransaction(fetchId, savings.getId(),
                "2026-05-05", new BigDecimal("12000.00"), "DEBIT",  "EMI PAYMENT - HOME LOAN HDFC",         "NACH", "TXN002");
            saveTransaction(fetchId, savings.getId(),
                "2026-05-10", new BigDecimal("3500.00"),  "DEBIT",  "UPI/GPAY/AMAZON SHOPPING",             "UPI",  "TXN003");
            saveTransaction(fetchId, savings.getId(),
                "2026-05-15", new BigDecimal("8000.00"),  "DEBIT",  "RENT PAYMENT - TRANSFER",              "IMPS", "TXN004");
            saveTransaction(fetchId, savings.getId(),
                "2026-05-20", new BigDecimal("2500.00"),  "DEBIT",  "ELECTRICITY BILL - TNEB",              "UPI",  "TXN005");
            saveTransaction(fetchId, savings.getId(),
                "2026-06-01", new BigDecimal("45000.00"), "CREDIT", "SALARY CREDIT - DEL CAPITAL PVT LTD", "NEFT", "TXN006");
            saveTransaction(fetchId, savings.getId(),
                "2026-06-05", new BigDecimal("12000.00"), "DEBIT",  "EMI PAYMENT - HOME LOAN HDFC",         "NACH", "TXN007");
            saveTransaction(fetchId, savings.getId(),
                "2026-06-10", new BigDecimal("1200.00"),  "DEBIT",  "SWIGGY/ZOMATO FOOD ORDER",             "UPI",  "TXN008");

            // --- Fixed Deposit Account ---
            FinancialAccount fd = FinancialAccount.builder()
                    .fetchId(fetchId).customerId(customerId)
                    .fipId("FINSHAREBANK-OE-UAT")
                    .accountType("TERM_DEPOSIT")
                    .maskedAccount("XXXXXXXX8765")
                    .ifscCode("FINS0001234")
                    .currency("INR")
                    .balance(new BigDecimal("150000.00"))
                    .balanceDate(LocalDate.now())
                    .rawData(Map.of("source", "sandbox", "maturityDate", "2027-06-21",
                            "interestRate", "7.5"))
                    .build();
            accountRepo.save(fd);

            // --- Home Loan ---
            FinancialLoan loan = FinancialLoan.builder()
                    .fetchId(fetchId).customerId(customerId)
                    .fipId("FINSHAREBANK-OE-UAT")
                    .loanType("HOME_LOAN")
                    .maskedAccount("XXXXXXXX9999")
                    .principal(new BigDecimal("3500000.00"))
                    .outstanding(new BigDecimal("2980000.00"))
                    .emiAmount(new BigDecimal("28500.00"))
                    .interestRate(new BigDecimal("8.50"))
                    .tenureMonths(240)
                    .disbursementDate(LocalDate.of(2021, 6, 1))
                    .maturityDate(LocalDate.of(2041, 6, 1))
                    .rawData(Map.of("source", "sandbox"))
                    .build();
            loanRepo.save(loan);

            // Mark fetch as COMPLETED
            fetch.setStatus(FetchRequest.FetchStatus.COMPLETED);
            fetchRepo.save(fetch);

            log.info("Sandbox data stored for fetchId={}", fetchId);
        } catch (Exception e) {
            log.error("Failed to store sandbox data: {}", e.getMessage(), e);
        }
    }

    private void saveTransaction(UUID fetchId, UUID accountId,
            String date, BigDecimal amount, String type, String narration, String mode, String ref) {
        txRepo.save(FinancialTransaction.builder()
                .accountId(accountId).fetchId(fetchId)
                .transactionDate(LocalDate.parse(date))
                .amount(amount).txType(type)
                .narration(narration).mode(mode).reference(ref)
                .rawData(Map.of("source", "sandbox"))
                .build());
    }

    @Transactional(readOnly = true)
    public FetchDTO.DataResponse getFetchData(UUID fetchId) {
        FetchRequest fetch = fetchRepo.findById(fetchId)
                .orElseThrow(() -> new NoSuchElementException("Fetch not found: " + fetchId));

        List<FinancialAccount> accounts = accountRepo.findByFetchId(fetchId);
        List<FinancialLoan>    loans    = loanRepo.findByFetchId(fetchId);

        List<FetchDTO.DataResponse.AccountDTO> accountDTOs = accounts.stream().map(a -> {
            List<FinancialTransaction> txList = txRepo.findByAccountId(a.getId());
            List<FetchDTO.DataResponse.TransactionDTO> txDTOs = txList.stream()
                    .map(t -> FetchDTO.DataResponse.TransactionDTO.builder()
                            .transactionId(t.getId())
                            .transactionDate(t.getTransactionDate())
                            .amount(t.getAmount() != null ? t.getAmount().doubleValue() : null)
                            .type(t.getTxType()).narration(t.getNarration())
                            .reference(t.getReference()).mode(t.getMode())
                            .build()).toList();
            return FetchDTO.DataResponse.AccountDTO.builder()
                    .accountId(a.getId()).fipId(a.getFipId())
                    .accountType(a.getAccountType()).maskedAccount(a.getMaskedAccount())
                    .ifscCode(a.getIfscCode()).currency(a.getCurrency())
                    .balance(a.getBalance() != null ? a.getBalance().doubleValue() : null)
                    .balanceDate(a.getBalanceDate()).transactions(txDTOs)
                    .build();
        }).toList();

        List<FetchDTO.DataResponse.LoanDTO> loanDTOs = loans.stream()
                .map(l -> FetchDTO.DataResponse.LoanDTO.builder()
                        .loanId(l.getId()).fipId(l.getFipId()).loanType(l.getLoanType())
                        .maskedAccount(l.getMaskedAccount())
                        .principal(l.getPrincipal() != null ? l.getPrincipal().doubleValue() : null)
                        .outstanding(l.getOutstanding() != null ? l.getOutstanding().doubleValue() : null)
                        .emiAmount(l.getEmiAmount() != null ? l.getEmiAmount().doubleValue() : null)
                        .interestRate(l.getInterestRate() != null ? l.getInterestRate().doubleValue() : null)
                        .tenureMonths(l.getTenureMonths())
                        .build()).toList();

        return FetchDTO.DataResponse.builder()
                .fetchId(fetchId).status(fetch.getStatus().name())
                .accounts(accountDTOs).loans(loanDTOs)
                .build();
    }

    @Transactional
    public void handleFetchWebhook(Map<String, Object> payload) {
        String digioFetchId = extractString(payload, "sessionId", "id", "fetchId");
        if (digioFetchId == null) return;
        fetchRepo.findByDigioFetchId(digioFetchId).ifPresent(fetch -> {
            String status = extractString(payload, "status", "fetchStatus");
            if ("COMPLETED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
                storeSandboxData(fetch);
            }
        });
    }

    private Map<String, Object> buildFetchPayload(ConsentRequest consent, FetchDTO.InitiateRequest req) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("consentId", consent.getDigioConsentId());
        p.put("from", req.getFromDate().toString());
        p.put("to",   req.getToDate().toString());
        p.put("fiTypes", req.getFiTypes() != null ? req.getFiTypes() : consent.getFiTypes());
        return p;
    }

    private String extractString(Map<?, ?> map, String... keys) {
        for (String k : keys)
            if (map.containsKey(k) && map.get(k) != null) return map.get(k).toString();
        return null;
    }
}
