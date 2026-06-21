package com.delcapital.aa.controller;

import com.delcapital.aa.dto.FetchDTO;
import com.delcapital.aa.service.DataFetchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/fetch")
@RequiredArgsConstructor
@Slf4j
public class DataFetchController {

    private final DataFetchService dataFetchService;

    /**
     * POST /api/v1/fetch
     * Initiate a data fetch for an active consent
     */
    @PostMapping
    public ResponseEntity<FetchDTO.Response> initiateFetch(
            @Valid @RequestBody FetchDTO.InitiateRequest request) {
        log.info("POST /v1/fetch - consentId={}", request.getConsentId());
        FetchDTO.Response response = dataFetchService.initiateFetch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/fetch/{fetchId}
     * Get status of a fetch request
     */
    @GetMapping("/{fetchId}")
    public ResponseEntity<FetchDTO.Response> getFetchStatus(@PathVariable UUID fetchId) {
        log.info("GET /v1/fetch/{}", fetchId);
        // Re-use DataResponse but just status
        FetchDTO.DataResponse data = dataFetchService.getFetchData(fetchId);
        // Return lightweight response
        FetchDTO.Response resp = FetchDTO.Response.builder()
                .fetchId(fetchId)
                .status(data.getStatus())
                .build();
        return ResponseEntity.ok(resp);
    }

    /**
     * GET /api/v1/fetch/{fetchId}/data
     * Get normalized financial data from a completed fetch
     */
    @GetMapping("/{fetchId}/data")
    public ResponseEntity<FetchDTO.DataResponse> getFetchData(@PathVariable UUID fetchId) {
        log.info("GET /v1/fetch/{}/data", fetchId);
        return ResponseEntity.ok(dataFetchService.getFetchData(fetchId));
    }
}
