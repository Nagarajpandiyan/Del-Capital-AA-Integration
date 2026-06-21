package com.delcapital.aa.controller;

import com.delcapital.aa.service.ConsentService;
import com.delcapital.aa.service.DataFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final ConsentService consentService;
    private final DataFetchService dataFetchService;

    /**
     * POST /api/v1/webhook/consent
     * Receives asynchronous consent status updates from Digio
     */
    @PostMapping("/consent")
    public ResponseEntity<Map<String, String>> consentWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Digio-Signature", required = false) String signature) {
        log.info("Consent webhook received: keys={}", payload.keySet());
        // TODO: verify HMAC signature in production
        consentService.handleWebhook(payload);
        return ResponseEntity.ok(Map.of("status", "received"));
    }

    /**
     * POST /api/v1/webhook/fetch
     * Receives asynchronous data fetch completion notifications from Digio
     */
    @PostMapping("/fetch")
    public ResponseEntity<Map<String, String>> fetchWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Digio-Signature", required = false) String signature) {
        log.info("Fetch webhook received: keys={}", payload.keySet());
        dataFetchService.handleFetchWebhook(payload);
        return ResponseEntity.ok(Map.of("status", "received"));
    }

    /**
     * GET /api/v1/webhook/health
     * Digio health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "aa-integration"));
    }
}
