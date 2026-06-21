package com.delcapital.aa.controller;

import com.delcapital.aa.dto.ConsentDTO;
import com.delcapital.aa.service.ConsentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/consent")
@RequiredArgsConstructor
@Slf4j
public class ConsentController {

    private final ConsentService consentService;

    /**
     * POST /api/v1/consent
     * Create a new consent request for a customer
     */
    @PostMapping
    public ResponseEntity<ConsentDTO.Response> createConsent(
            @Valid @RequestBody ConsentDTO.CreateRequest request) {
        log.info("POST /v1/consent - purposeCode={}", request.getPurposeCode());
        ConsentDTO.Response response = consentService.createConsent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/consent/{consentId}
     * Get status of a consent request
     */
    @GetMapping("/{consentId}")
    public ResponseEntity<ConsentDTO.StatusResponse> getConsentStatus(
            @PathVariable UUID consentId) {
        log.info("GET /v1/consent/{}", consentId);
        return ResponseEntity.ok(consentService.getConsentStatus(consentId));
    }

    /**
     * GET /api/v1/consent?mobile={mobileNumber}
     * List all consents for a customer
     */
    @GetMapping
    public ResponseEntity<List<ConsentDTO.StatusResponse>> listConsents(
            @RequestParam String mobile) {
        return ResponseEntity.ok(consentService.listByMobile(mobile));
    }
}
