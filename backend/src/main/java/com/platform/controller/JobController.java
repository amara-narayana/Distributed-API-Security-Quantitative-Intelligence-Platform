package com.platform.controller;

import com.platform.service.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private static final Logger log = LoggerFactory.getLogger(JobController.class);

    @Autowired
    private JobScheduler jobScheduler;

    @PostMapping("/assign")
    public ResponseEntity<?> assignJob(@RequestBody Map<String, Object> payload) {
        log.info("Received job assignment request");

        try {
            String jobType = (String) payload.get("jobType");
            Object jobPayload = payload.get("payload");
            String region = (String) payload.get("region");

            UUID jobId = UUID.randomUUID();
            jobScheduler.assignJob(jobId, jobType, jobPayload, region);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Job assigned successfully",
                    "jobId", jobId,
                    "jobType", jobType,
                    "status", "ASSIGNED"
            ));
        } catch (Exception e) {
            log.error("Failed to assign job: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/submit-result")
    public ResponseEntity<?> submitResult(@RequestBody Map<String, Object> payload) {
        log.info("Received job result submission");

        try {
            String jobIdStr = (String) payload.get("jobId");
            boolean success = "COMPLETED".equals(payload.get("status"));
            String errorMessage = (String) payload.get("errorMessage");

            UUID jobId = UUID.fromString(jobIdStr);
            jobScheduler.completeJob(jobId, success, errorMessage);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Result submitted successfully",
                    "jobId", jobId
            ));
        } catch (Exception e) {
            log.error("Failed to submit result: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
