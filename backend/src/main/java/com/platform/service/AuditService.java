package com.platform.service;

import com.platform.entity.AuditLog;
import com.platform.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired(required = false)
    private BlockchainAuditService blockchainAuditService;

    @Async
    public void logAudit(String actor, String action, String targetResource, Object metadata) {
        log.info("Logging audit entry: {} - {} -> {}", actor, action, targetResource);

        try {
            String entryId = generateEntryId(actor, action, Instant.now());
            String resultHash = computeHash(actor, action, targetResource, metadata);

            AuditLog auditLog = AuditLog.builder()
                    .entryId(entryId)
                    .actor(actor)
                    .action(action)
                    .targetResource(targetResource)
                    .resultHash(resultHash)
                    .metadata(metadata != null ? metadata.toString() : null)
                    .build();

            AuditLog savedLog = auditLogRepository.save(auditLog);
            log.info("Audit log saved with ID: {}", savedLog.getId());

            if (blockchainAuditService != null) {
                submitToBlockchain(savedLog);
            } else {
                log.debug("Blockchain audit service not available, skipping blockchain submission");
            }

        } catch (Exception e) {
            log.error("Failed to log audit entry: {}", e.getMessage(), e);
            throw new RuntimeException("Audit logging failed", e);
        }
    }

    private void submitToBlockchain(AuditLog auditLog) {
        try {
            log.info("Submitting audit entry to blockchain: {}", auditLog.getEntryId());
            String txId = blockchainAuditService.storeAuditEntry(
                    auditLog.getEntryId(),
                    auditLog.getActor(),
                    auditLog.getAction(),
                    auditLog.getTargetResource(),
                    auditLog.getResultHash(),
                    auditLog.getMetadata()
            );

            auditLog.setBlockchainTxId(txId);
            auditLogRepository.save(auditLog);
            log.info("Blockchain transaction confirmed: {}", txId);

        } catch (Exception e) {
            log.error("Failed to submit to blockchain: {}", e.getMessage());
        }
    }

    public AuditLog getAuditEntry(String entryId) {
        return auditLogRepository.findByEntryId(entryId)
                .orElseThrow(() -> new RuntimeException("Audit entry not found: " + entryId));
    }

    public boolean verifyAuditEntry(String entryId) {
        try {
            AuditLog auditLog = getAuditEntry(entryId);

            String expectedHash = computeHash(
                    auditLog.getActor(),
                    auditLog.getAction(),
                    auditLog.getTargetResource(),
                    auditLog.getMetadata()
            );

            boolean valid = expectedHash.equals(auditLog.getResultHash());
            log.info("Audit entry {} verification: {}", entryId, valid ? "VALID" : "INVALID");

            if (auditLog.getBlockchainTxId() != null && blockchainAuditService != null) {
                boolean blockchainValid = blockchainAuditService.verifyTransaction(auditLog.getBlockchainTxId());
                log.info("Blockchain verification for {}: {}", entryId, blockchainValid ? "VALID" : "INVALID");
                return valid && blockchainValid;
            }

            return valid;

        } catch (Exception e) {
            log.error("Verification failed for {}: {}", entryId, e.getMessage());
            return false;
        }
    }

    private String generateEntryId(String actor, String action, Instant timestamp) {
        return String.format("AUDIT-%s-%s-%d",
                actor.toUpperCase().replaceAll("[^A-Z0-9]", "_"),
                action.toUpperCase().replaceAll("[^A-Z0-9]", "_"),
                timestamp.toEpochMilli());
    }

    private String computeHash(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder input = new StringBuilder();
            for (String value : values) {
                if (value != null) {
                    input.append(value);
                }
            }
            byte[] hashBytes = digest.digest(input.toString().getBytes());
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
