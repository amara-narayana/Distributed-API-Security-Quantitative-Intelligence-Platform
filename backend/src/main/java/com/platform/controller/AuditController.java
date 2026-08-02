package com.platform.controller;

import com.platform.entity.AuditLog;
import com.platform.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private static final Logger log = LoggerFactory.getLogger(AuditController.class);

    @Autowired
    private AuditService auditService;

    @GetMapping("/{entryId}")
    public ResponseEntity<?> getAuditEntry(@PathVariable String entryId) {
        log.info("Fetching audit entry: {}", entryId);

        try {
            AuditLog auditLog = auditService.getAuditEntry(entryId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "entry", auditLog
            ));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.error("Failed to fetch audit entry: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/verify/{entryId}")
    public ResponseEntity<?> verifyAuditEntry(@PathVariable String entryId) {
        log.info("Verifying audit entry: {}", entryId);

        try {
            boolean isValid = auditService.verifyAuditEntry(entryId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("entryId", entryId);
            response.put("valid", isValid);
            response.put("message", isValid ? "Audit entry verified successfully" : "Verification failed");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to verify audit entry: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/query")
    public ResponseEntity<?> queryAuditLogs(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "20") int limit) {
        
        log.info("Querying audit logs with actor={}, action={}", actor, action);

        try {
            List<AuditLog> results;

            if (actor != null && !actor.isEmpty()) {
                results = auditService.findByActor(actor);
            } else if (action != null && !action.isEmpty()) {
                results = auditService.findByAction(action);
            } else {
                results = auditService.findAll();
            }

            if (results.size() > limit) {
                results = results.subList(0, limit);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", results.size(),
                    "entries", results
            ));
        } catch (Exception e) {
            log.error("Failed to query audit logs: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/blockchain-anchored")
    public ResponseEntity<?> getBlockchainAnchoredEntries() {
        try {
            List<AuditLog> anchoredEntries = auditService.findAllWithBlockchainTx();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", anchoredEntries.size(),
                    "entries", anchoredEntries
            ));
        } catch (Exception e) {
            log.error("Failed to fetch blockchain-anchored entries: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
