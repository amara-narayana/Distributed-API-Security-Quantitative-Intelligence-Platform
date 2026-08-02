package com.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates IDOR (Insecure Direct Object Reference) test variations.
 * Creates different ID patterns to test for authorization vulnerabilities.
 */
public class IdorTestGenerator {

    private static final Logger logger = LoggerFactory.getLogger(IdorTestGenerator.class);

    /**
     * Generate a list of ID variations for testing IDOR vulnerabilities.
     * @param originalId The original ID found in the response
     * @return List of ID variations to test
     */
    public List<String> generateIdVariations(String originalId) {
        List<String> variations = new ArrayList<>();
        
        logger.info("Generating ID variations for: {}", originalId);
        
        // Add original ID as baseline
        variations.add(originalId);
        
        // Try numeric variations if ID is numeric
        if (isNumeric(originalId)) {
            long numericId = Long.parseLong(originalId);
            variations.addAll(generateNumericVariations(numericId));
        }
        
        // Try UUID variations if ID looks like UUID
        if (isUuid(originalId)) {
            variations.addAll(generateUuidVariations(originalId));
        }
        
        // Common vulnerable IDs
        variations.addAll(getCommonVulnerableIds());
        
        // Negative numbers
        variations.add("-1");
        variations.add("-999");
        
        return variations;
    }

    /**
     * Generate sequential ID variations (increment/decrement).
     * @param baseId Base numeric ID
     * @return List of sequential variations
     */
    private List<String> generateNumericVariations(long baseId) {
        List<String> variations = new ArrayList<>();
        
        // Increment by 1
        variations.add(String.valueOf(baseId + 1));
        
        // Decrement by 1
        if (baseId > 0) {
            variations.add(String.valueOf(baseId - 1));
        }
        
        // Increment by small amounts
        variations.add(String.valueOf(baseId + 2));
        variations.add(String.valueOf(baseId + 10));
        variations.add(String.valueOf(baseId + 100));
        
        // Decrement by small amounts
        if (baseId > 10) {
            variations.add(String.valueOf(baseId - 10));
        }
        
        // Boundary values
        variations.add("0");
        variations.add("1");
        variations.add("999999999");
        
        return variations;
    }

    /**
     * Generate UUID variations for testing.
     * @param originalUuid Original UUID string
     * @return List of UUID variations
     */
    private List<String> generateUuidVariations(String originalUuid) {
        List<String> variations = new ArrayList<>();
        
        try {
            UUID original = UUID.fromString(originalUuid.replace("-", ""));
            long mostSigBits = original.getMostSignificantBits();
            long leastSigBits = original.getLeastSignificantBits();
            
            // Variations with modified bits
            variations.add(new UUID(mostSigBits + 1, leastSigBits).toString());
            variations.add(new UUID(mostSigBits - 1, leastSigBits).toString());
            variations.add(new UUID(mostSigBits, leastSigBits + 1).toString());
            variations.add(new UUID(mostSigBits, leastSigBits - 1).toString());
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid UUID format: {}", originalUuid);
        }
        
        // Generate some random UUIDs
        for (int i = 0; i < 5; i++) {
            variations.add(UUID.randomUUID().toString());
        }
        
        return variations;
    }

    /**
     * Get commonly vulnerable IDs used in testing.
     * @return List of common test IDs
     */
    private List<String> getCommonVulnerableIds() {
        return Arrays.asList(
            "1", "2", "3", "4", "5",
            "100", "1000", "10000",
            "admin", "administrator", "root",
            "test", "user", "guest"
        );
    }

    /**
     * Check if a string is numeric.
     * @param str String to check
     * @return true if numeric
     */
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Long.parseLong(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Check if a string is a UUID.
     * @param str String to check
     * @return true if UUID format
     */
    private boolean isUuid(String str) {
        if (str == null || str.length() < 32) {
            return false;
        }
        String uuidPattern = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
        Pattern pattern = Pattern.compile(uuidPattern);
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }

    /**
     * Generate test payloads for IDOR testing.
     * @param endpoint Target endpoint
     * @param paramName Parameter name containing the ID
     * @param originalId Original ID value
     * @return List of test payloads
     */
    public List<TestPayload> generateTestPayloads(String endpoint, String paramName, String originalId) {
        List<TestPayload> payloads = new ArrayList<>();
        List<String> idVariations = generateIdVariations(originalId);
        
        for (String variation : idVariations) {
            TestPayload payload = new TestPayload(endpoint, paramName, variation, originalId);
            payloads.add(payload);
        }
        
        return payloads;
    }

    /**
     * Inner class representing a test payload.
     */
    public static class TestPayload {
        private final String endpoint;
        private final String paramName;
        private final String testValue;
        private final String originalValue;

        public TestPayload(String endpoint, String paramName, String testValue, String originalValue) {
            this.endpoint = endpoint;
            this.paramName = paramName;
            this.testValue = testValue;
            this.originalValue = originalValue;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public String getParamName() {
            return paramName;
        }

        public String getTestValue() {
            return testValue;
        }

        public String getOriginalValue() {
            return originalValue;
        }

        @Override
        public String toString() {
            return String.format("TestPayload{endpoint=%s, param=%s, testValue=%s}", endpoint, paramName, testValue);
        }
    }
}
