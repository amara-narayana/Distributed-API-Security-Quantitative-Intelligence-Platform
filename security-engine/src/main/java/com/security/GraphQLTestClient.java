package com.security;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * GraphQL test client for security testing.
 * Sends introspection queries and mutation tests to GraphQL endpoints.
 */
public class GraphQLTestClient {

    private static final Logger logger = LoggerFactory.getLogger(GraphQLTestClient.class);
    private final OkHttpClient httpClient;

    public GraphQLTestClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Perform GraphQL introspection to discover schema.
     * @param endpoint GraphQL endpoint URL
     * @return Schema information as JSON string
     */
    public String introspect(String endpoint) {
        logger.info("Performing GraphQL introspection on: {}", endpoint);
        
        String introspectionQuery = "{" +
            "__schema {" +
                "queryType { name }," +
                "mutationType { name }," +
                "subscriptionType { name }," +
                "types {" +
                    "name," +
                    "kind," +
                    "fields {" +
                        "name," +
                        "type { name kind }" +
                    "}" +
                "}" +
            "}" +
        "}";
        
        return executeQuery(endpoint, introspectionQuery);
    }

    /**
     * Execute a GraphQL query.
     * @param endpoint GraphQL endpoint URL
     * @param query GraphQL query string
     * @return Response as JSON string
     */
    public String executeQuery(String endpoint, String query) {
        logger.debug("Executing GraphQL query: {}", query.substring(0, Math.min(100, query.length())));
        
        try {
            String jsonBody = String.format("{\"query\": %s}", jsonString(query));
            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
            
            Request request = new Request.Builder()
                    .url(endpoint)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.body() != null) {
                    String responseBody = response.body().string();
                    logger.debug("GraphQL response: {}", responseBody.substring(0, Math.min(200, responseBody.length())));
                    return responseBody;
                } else {
                    logger.warn("Empty response from GraphQL endpoint");
                    return "{}";
                }
            }
            
        } catch (IOException e) {
            logger.error("Error executing GraphQL query", e);
            return "{\"errors\": [{\"message\": \"" + e.getMessage() + "\"}]}";
        }
    }

    /**
     * Test for common GraphQL vulnerabilities.
     * @param endpoint GraphQL endpoint URL
     * @return List of discovered vulnerabilities
     */
    public List<GraphQLVulnerability> testVulnerabilities(String endpoint) {
        logger.info("Testing GraphQL vulnerabilities on: {}", endpoint);
        
        java.util.ArrayList<GraphQLVulnerability> vulnerabilities = new java.util.ArrayList<>();
        
        // Test 1: Check if introspection is enabled (information disclosure)
        String introspectionResult = introspect(endpoint);
        if (introspectionResult.contains("__schema") && !introspectionResult.contains("errors")) {
            vulnerabilities.add(new GraphQLVulnerability(
                "INTROSPECTION_ENABLED",
                "LOW",
                "GraphQL introspection is enabled, exposing schema details"
            ));
        }
        
        // Test 2: Try depth limit attack
        String deepQuery = buildDeepQuery(10);
        String deepResult = executeQuery(endpoint, deepQuery);
        if (!deepResult.contains("errors") || !deepResult.contains("depth")) {
            vulnerabilities.add(new GraphQLVulnerability(
                "NO_DEPTH_LIMIT",
                "MEDIUM",
                "No query depth limiting detected, potential DoS vulnerability"
            ));
        }
        
        // Test 3: Try batch query attack
        String batchResult = testBatchQuery(endpoint);
        if (!batchResult.contains("errors")) {
            vulnerabilities.add(new GraphQLVulnerability(
                "NO_BATCH_LIMIT",
                "MEDIUM",
                "No batch query limiting detected"
            ));
        }
        
        return vulnerabilities;
    }

    /**
     * Build a deeply nested query for testing depth limits.
     * @param depth Query depth
     * @return Deeply nested GraphQL query
     */
    private String buildDeepQuery(int depth) {
        StringBuilder query = new StringBuilder("{");
        for (int i = 0; i < depth; i++) {
            query.append("user").append(i).append("{");
        }
        query.append("id");
        for (int i = 0; i < depth; i++) {
            query.append("}");
        }
        return query.toString();
    }

    /**
     * Test batch query functionality.
     * @param endpoint GraphQL endpoint
     * @return Response from batch query
     */
    private String testBatchQuery(String endpoint) {
        String batchQuery = "[" +
            "{\"query\": \"{__typename}\"}," +
            "{\"query\": \"{__typename}\"}," +
            "{\"query\": \"{__typename}\"}" +
        "]";
        
        try {
            RequestBody body = RequestBody.create(batchQuery, MediaType.parse("application/json"));
            
            Request request = new Request.Builder()
                    .url(endpoint)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.body() != null) {
                    return response.body().string();
                }
            }
        } catch (IOException e) {
            logger.warn("Batch query test failed", e);
        }
        
        return "{\"errors\": []}";
    }

    /**
     * Escape a string for JSON.
     * @param s String to escape
     * @return Escaped JSON string
     */
    private String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t") + "\"";
    }

    /**
     * Inner class representing a GraphQL vulnerability.
     */
    public static class GraphQLVulnerability {
        private final String type;
        private final String severity;
        private final String description;

        public GraphQLVulnerability(String type, String severity, String description) {
            this.type = type;
            this.severity = severity;
            this.description = description;
        }

        public String getType() {
            return type;
        }

        public String getSeverity() {
            return severity;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return String.format("GraphQLVulnerability{type=%s, severity=%s}", type, severity);
        }
    }
}
