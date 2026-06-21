package com.delcapital.aa.service;

import com.delcapital.aa.dto.ConsentDTO;
import com.delcapital.aa.model.ConsentRequest;
import com.delcapital.aa.model.Customer;
import com.delcapital.aa.repository.ConsentRequestRepository;
import com.delcapital.aa.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConsentService {

    private final ConsentRequestRepository consentRepo;
    private final CustomerRepository customerRepo;
    private final DigioApiClient digioClient;
    private final AuditService auditService;

    @Value("${digio.template-id}")
    private String templateId;

    @Value("${app.consent.redirect-url}")
    private String defaultRedirectUrl;

    @Value("${app.consent.expiry-days:30}")
    private int consentExpiryDays;

    @Transactional
    public ConsentDTO.Response createConsent(ConsentDTO.CreateRequest req) {
        log.info("Creating consent for purposeCode={}", req.getPurposeCode());

        String mobileHash = sha256(req.getMobileNumber());
        Customer customer = customerRepo.findByMobileHash(mobileHash)
                .orElseGet(() -> customerRepo.save(Customer.builder()
                        .mobileHash(mobileHash)
                        .emailHash(req.getEmail() != null ? sha256(req.getEmail()) : null)
                        .aaHandle(req.getAaHandle())
                        .build()));

        LocalDate from = req.getDateRangeFrom() != null ? req.getDateRangeFrom() : LocalDate.now().minusMonths(6);
        LocalDate to   = req.getDateRangeTo()   != null ? req.getDateRangeTo()   : LocalDate.now();

        Map<String, Object> payload = digioClient.buildConsentPayload(
                req.getAaHandle(), req.getMobileNumber(),
                req.getPurposeCode(), req.getPurposeText(),
                req.getFiTypes(), from.toString(), to.toString(), defaultRedirectUrl);

        // Try Digio — if it fails/returns sandbox mock, we still continue
        Map<String, Object> digioResponse = new HashMap<>();
        String digioConsentId = null;
        String digioRedirectUrl = null;

        try {
            digioResponse = digioClient.createConsent(payload);
            digioConsentId  = extractString(digioResponse, "id", "consentId", "consent_id");
            digioRedirectUrl = extractString(digioResponse, "redirectUrl", "redirect_url", "url");
            log.info("Digio response: id={}", digioConsentId);
        } catch (Exception e) {
            log.warn("Digio call failed (sandbox limitation): {}", e.getMessage());
        }

        // Determine status:
        // - If Digio returned a real consentId → PENDING (wait for webhook)
        // - If Digio sandbox returned mock/failed → ACTIVE directly (sandbox mode)
        boolean isSandboxMock = (digioConsentId == null)
                || digioConsentId.startsWith("SANDBOX_")
                || Boolean.TRUE.equals(digioResponse.get("sandbox"));

        ConsentRequest.ConsentStatus initialStatus = isSandboxMock
                ? ConsentRequest.ConsentStatus.ACTIVE
                : ConsentRequest.ConsentStatus.PENDING;

        if (isSandboxMock) {
            log.info("Sandbox mode: setting consent to ACTIVE directly");
            // Generate a local ID if Digio didn't return one
            if (digioConsentId == null) {
                digioConsentId = "SANDBOX_" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
            }
        }

        ConsentRequest consent = ConsentRequest.builder()
                .customer(customer)
                .templateId(templateId)
                .status(initialStatus)
                .purposeCode(req.getPurposeCode())
                .purposeText(req.getPurposeText())
                .fiTypes(req.getFiTypes())
                .dateRangeFrom(from)
                .dateRangeTo(to)
                .consentExpiry(OffsetDateTime.now().plusDays(consentExpiryDays))
                .redirectUrl(defaultRedirectUrl)
                .digioConsentId(digioConsentId)
                .digioRedirectUrl(digioRedirectUrl)
                .rawRequest(payload)
                .rawResponse(digioResponse)
                .build();

        consent = consentRepo.save(consent);
        log.info("Consent saved: id={}, status={}", consent.getId(), consent.getStatus());

        auditService.log("consent_request", consent.getId(), "CREATED", "system",
                null, Map.of("status", initialStatus.name(), "purposeCode", req.getPurposeCode()));

        return ConsentDTO.Response.from(consent);
    }

    @Transactional
    public ConsentDTO.StatusResponse getConsentStatus(UUID consentId) {
        ConsentRequest consent = consentRepo.findById(consentId)
                .orElseThrow(() -> new NoSuchElementException("Consent not found: " + consentId));

        // Only poll Digio for real (non-sandbox) pending consents
        if (consent.getStatus() == ConsentRequest.ConsentStatus.PENDING
                && consent.getDigioConsentId() != null
                && !consent.getDigioConsentId().startsWith("SANDBOX_")) {
            try {
                Map<String, Object> s = digioClient.getConsentStatus(consent.getDigioConsentId());
                updateConsentFromDigioStatus(consent, s);
                consentRepo.save(consent);
            } catch (Exception e) {
                log.warn("Failed to refresh from Digio: {}", e.getMessage());
            }
        }
        return ConsentDTO.StatusResponse.from(consent);
    }

    @Transactional
    public void handleWebhook(Map<String, Object> payload) {
        String digioConsentId = extractString(payload, "consentId", "id", "consent_id");
        if (digioConsentId == null) return;
        consentRepo.findByDigioConsentId(digioConsentId).ifPresent(consent -> {
            ConsentRequest.ConsentStatus old = consent.getStatus();
            updateConsentFromDigioStatus(consent, payload);
            consentRepo.save(consent);
            auditService.log("consent_request", consent.getId(), "WEBHOOK_STATUS_UPDATE",
                    "digio-webhook", Map.of("status", old), Map.of("status", consent.getStatus()));
        });
    }

    public List<ConsentDTO.StatusResponse> listByMobile(String mobileNumber) {
        String mobileHash = sha256(mobileNumber);
        return customerRepo.findByMobileHash(mobileHash)
                .map(c -> consentRepo.findByCustomer(c).stream()
                        .map(ConsentDTO.StatusResponse::from).toList())
                .orElse(List.of());
    }

    private void updateConsentFromDigioStatus(ConsentRequest consent, Map<String, Object> data) {
        Object s = data.getOrDefault("status", data.get("consentStatus"));
        if (s == null) return;
        try {
            consent.setStatus(switch (s.toString().toUpperCase()) {
                case "ACTIVE", "APPROVED" -> ConsentRequest.ConsentStatus.ACTIVE;
                case "PAUSED"             -> ConsentRequest.ConsentStatus.PAUSED;
                case "REVOKED","REJECTED" -> ConsentRequest.ConsentStatus.REVOKED;
                case "EXPIRED"            -> ConsentRequest.ConsentStatus.EXPIRED;
                case "FAILED","ERROR"     -> ConsentRequest.ConsentStatus.FAILED;
                default -> consent.getStatus();
            });
        } catch (Exception e) {
            log.warn("Unknown status from Digio: {}", s);
        }
    }

    private String extractString(Map<String, Object> map, String... keys) {
        for (String k : keys)
            if (map.containsKey(k) && map.get(k) != null) return map.get(k).toString();
        return null;
    }

    private String sha256(String input) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException("SHA-256 failed", e); }
    }
}
