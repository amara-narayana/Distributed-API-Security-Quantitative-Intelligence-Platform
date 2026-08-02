package com.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes test results to detect vulnerabilities and PII data.
 * Checks status codes, response patterns, and scans for sensitive information.
 */
public class ResultAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(ResultAnalyzer.class);

    // PII detection patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "\\+?[1-9]\\d{1,14}|\\(\\d{3}\\)\\s?\\d{3}-\\d{4}|\\d{3}-\\d{3}-\\d{4}"
    );
    
    private static final Pattern SSN_PATTERN = Pattern.compile(
        "\\b\\d{3}-\\d{2}-\\d{4}\\b"
    );
    
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
        "\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13})\\b"
    );
    
    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile(
        "\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b"
    );

    /**
     * Analyze a test response for vulnerabilities.
     * @param statusCode HTTP status code
     * @param responseBody Response body content
     * @param originalId Original ID used in request
     * @param testId Test ID used in request
     * @return Analysis result
     */
    public AnalysisResult analyzeResponse(int statusCode, String responseBody, 
                                          String originalId, String testId) {
        logger.debug("Analyzing response: status={}, bodyLength={}", statusCode, 
                     responseBody != null ? responseBody.length() : 0);
        
        AnalysisResult result = new AnalysisResult();
        result.setStatusCode(statusCode);
        result.setTestId(testId);
        result.setOriginalId(originalId);
        
        // Check for IDOR vulnerability
        if (isIdorVulnerability(statusCode, responseBody, originalId, testId)) {
            result.setVulnerabilityFound(true);
            result.setVulnerabilityType("IDOR");
            result.setSeverity("HIGH");
            result.addDetail("Different resource returned for different user ID");
        }
        
        // Check for BOLA vulnerability
        if (isBolaVulnerability(statusCode, responseBody)) {
            result.setVulnerabilityFound(true);
            result.setVulnerabilityType("BOLA");
            result.setSeverity("HIGH");
            result.addDetail("Broken Object Level Authorization detected");
        }
        
        // Scan for PII in response
        List<PiiFinding> piiFindings = scanForPii(responseBody);
        if (!piiFindings.isEmpty()) {
            result.setPiiFound(true);
            result.setPiiFindings(piiFindings);
            result.addDetail("PII data exposed in response: " + piiFindings.size() + " findings");
            
            if (!result.isVulnerabilityFound()) {
                result.setSeverity("MEDIUM");
            }
        }
        
        // Check for error messages that leak information
        if (containsInformationLeak(responseBody)) {
            result.addDetail("Potential information leakage in error message");
            result.setSeverity(result.getSeverity().equals("HIGH") ? "HIGH" : "MEDIUM");
        }
        
        return result;
    }

    /**
     * Detect IDOR vulnerability based on response patterns.
     */
    private boolean isIdorVulnerability(int statusCode, String responseBody, 
                                        String originalId, String testId) {
        // If we get 200 OK with a different ID than requested
        if (statusCode == 200 && responseBody != null) {
            // Check if response contains the test ID instead of original ID
            if (!testId.equals(originalId) && responseBody.contains(testId)) {
                return true;
            }
            // Check if we can access another user's data
            if (responseBody.contains("userId") || responseBody.contains("\"id\"")) {
                return !testId.equals(originalId);
            }
        }
        
        // 403 or 401 when accessing own resource indicates proper auth
        // 200 when accessing others' resource indicates vulnerability
        if ((statusCode == 200 || statusCode == 201) && !testId.equals(originalId)) {
            return true;
        }
        
        return false;
    }

    /**
     * Detect BOLA vulnerability.
     */
    private boolean isBolaVulnerability(int statusCode, String responseBody) {
        // Successful access to resource without proper authorization
        if (statusCode == 200 && responseBody != null) {
            // Check for common BOLA indicators
            return responseBody.contains("authorization") || 
                   responseBody.contains("permission") ||
                   responseBody.contains("access");
        }
        return false;
    }

    /**
     * Scan response for PII data.
     * @param responseBody Response text
     * @return List of PII findings
     */
    public List<PiiFinding> scanForPii(String responseBody) {
        List<PiiFinding> findings = new ArrayList<>();
        
        if (responseBody == null || responseBody.isEmpty()) {
            return findings;
        }
        
        // Scan for emails
        Matcher emailMatcher = EMAIL_PATTERN.matcher(responseBody);
        while (emailMatcher.find()) {
            findings.add(new PiiFinding("EMAIL", emailMatcher.group(), "Email address exposed"));
        }
        
        // Scan for phone numbers
        Matcher phoneMatcher = PHONE_PATTERN.matcher(responseBody);
        while (phoneMatcher.find()) {
            findings.add(new PiiFinding("PHONE", phoneMatcher.group(), "Phone number exposed"));
        }
        
        // Scan for SSN
        Matcher ssnMatcher = SSN_PATTERN.matcher(responseBody);
        while (ssnMatcher.find()) {
            findings.add(new PiiFinding("SSN", ssnMatcher.group(), "Social Security Number exposed"));
        }
        
        // Scan for credit cards
        Matcher ccMatcher = CREDIT_CARD_PATTERN.matcher(responseBody);
        while (ccMatcher.find()) {
            findings.add(new PiiFinding("CREDIT_CARD", ccMatcher.group(), "Credit card number exposed"));
        }
        
        // Scan for IP addresses
        Matcher ipMatcher = IP_ADDRESS_PATTERN.matcher(responseBody);
        while (ipMatcher.find()) {
            findings.add(new PiiFinding("IP_ADDRESS", ipMatcher.group(), "IP address exposed"));
        }
        
        if (!findings.isEmpty()) {
            logger.warn("Found {} PII instances in response", findings.size());
        }
        
        return findings;
    }

    /**
     * Check if response contains information leakage.
     */
    private boolean containsInformationLeak(String responseBody) {
        if (responseBody == null) {
            return false;
        }
        
        String[] leakPatterns = {
            "stack trace", "exception", "error details",
            "internal server", "database error", "sql",
            "password", "secret", "api_key", "token"
        };
        
        String lowerBody = responseBody.toLowerCase();
        for (String pattern : leakPatterns) {
            if (lowerBody.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Inner class representing analysis result.
     */
    public static class AnalysisResult {
        private int statusCode;
        private String testId;
        private String originalId;
        private boolean vulnerabilityFound;
        private String vulnerabilityType;
        private String severity;
        private boolean piiFound;
        private List<String> details = new ArrayList<>();
        private List<PiiFinding> piiFindings = new ArrayList<>();

        public int getStatusCode() { return statusCode; }
        public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
        
        public String getTestId() { return testId; }
        public void setTestId(String testId) { this.testId = testId; }
        
        public String getOriginalId() { return originalId; }
        public void setOriginalId(String originalId) { this.originalId = originalId; }
        
        public boolean isVulnerabilityFound() { return vulnerabilityFound; }
        public void setVulnerabilityFound(boolean found) { this.vulnerabilityFound = found; }
        
        public String getVulnerabilityType() { return vulnerabilityType; }
        public void setVulnerabilityType(String type) { this.vulnerabilityType = type; }
        
        public String getSeverity() { return severity != null ? severity : "INFO"; }
        public void setSeverity(String severity) { this.severity = severity; }
        
        public boolean isPiiFound() { return piiFound; }
        public void setPiiFound(boolean found) { this.piiFound = found; }
        
        public List<String> getDetails() { return details; }
        public void addDetail(String detail) { this.details.add(detail); }
        
        public List<PiiFinding> getPiiFindings() { return piiFindings; }
        public void setPiiFindings(List<PiiFinding> findings) { this.piiFindings = findings; }
    }

    /**
     * Inner class representing a PII finding.
     */
    public static class PiiFinding {
        private final String type;
        private final String value;
        private final String description;

        public PiiFinding(String type, String value, String description) {
            this.type = type;
            this.value = value;
            this.description = description;
        }

        public String getType() { return type; }
        public String getValue() { return value; }
        public String getDescription() { return description; }
    }
}
