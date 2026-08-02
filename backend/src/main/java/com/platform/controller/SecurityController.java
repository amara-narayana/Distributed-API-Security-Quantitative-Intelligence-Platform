package com.platform.controller;

import com.platform.dto.TestExecutionRequest;
import com.platform.entity.TestResult;
import com.platform.repository.TestResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/security")
public class SecurityController {

    private static final Logger log = LoggerFactory.getLogger(SecurityController.class);

    @Autowired
    private TestResultRepository testResultRepository;

    @PostMapping("/run")
    public ResponseEntity<?> runSecurityTest(@RequestBody TestExecutionRequest request) {
        log.info("Received security test request for target: {}", request.getTargetUrl());

        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Security test initiated");
            response.put("targetUrl", request.getTargetUrl());
            response.put("testTypes", request.getTestTypes());
            response.put("status", "RUNNING");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to run security test: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/results")
    public ResponseEntity<?> getResults(
            @RequestParam(required = false) String testType,
            @RequestParam(required = false) String severity,
            @RequestParam(defaultValue = "10") int limit) {
        
        try {
            List<TestResult> results;

            if (severity != null && !severity.isEmpty()) {
                TestResult.Severity sev = TestResult.Severity.valueOf(severity.toUpperCase());
                results = testResultRepository.findBySeverity(sev);
            } else if (testType != null && !testType.isEmpty()) {
                results = testResultRepository.findByTestType(testType);
            } else {
                results = testResultRepository.findAll();
            }

            if (results.size() > limit) {
                results = results.subList(0, limit);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", results.size(),
                    "results", results
            ));
        } catch (Exception e) {
            log.error("Failed to fetch test results: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/vulnerabilities")
    public ResponseEntity<?> getVulnerabilities() {
        try {
            List<TestResult> vulnerabilities = testResultRepository.findRecentVulnerabilities();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", vulnerabilities.size(),
                    "vulnerabilities", vulnerabilities
            ));
        } catch (Exception e) {
            log.error("Failed to fetch vulnerabilities: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
