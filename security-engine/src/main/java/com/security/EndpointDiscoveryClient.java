package com.security;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Client for discovering API endpoints via the AI Agent service.
 * Calls the FastAPI AI agent to get discovered schemas and endpoints.
 */
public class EndpointDiscoveryClient {

    private static final Logger logger = LoggerFactory.getLogger(EndpointDiscoveryClient.class);
    private final OkHttpClient httpClient;
    private final String aiAgentUrl;

    public EndpointDiscoveryClient(String aiAgentUrl) {
        this.aiAgentUrl = aiAgentUrl;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Discover endpoints for a given target domain using the AI agent.
     * @param targetDomain The domain to discover (e.g., "api.example.com")
     * @return List of discovered endpoints
     */
    public List<String> discoverEndpoints(String targetDomain) {
        logger.info("Discovering endpoints for domain: {}", targetDomain);
        
        try {
            String url = aiAgentUrl + "/discover";
            
            String jsonBody = String.format("{\"targetDomain\": \"%s\"}", targetDomain);
            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
            
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    logger.info("Discovery response: {}", responseBody);
                    
                    // Parse response to extract endpoints
                    // In production, use Jackson to parse properly
                    return parseEndpointsFromResponse(responseBody);
                } else {
                    logger.warn("Discovery failed with code: {}", response.code());
                    return Collections.emptyList();
                }
            }
            
        } catch (IOException e) {
            logger.error("Error discovering endpoints", e);
            return Collections.emptyList();
        }
    }

    /**
     * Extract data from a target domain (products, prices, etc.)
     * @param targetDomain The domain to extract data from
     * @param dataType Type of data to extract (e.g., "products", "prices")
     * @return Extracted data as JSON string
     */
    public String extractData(String targetDomain, String dataType) {
        logger.info("Extracting {} data from domain: {}", dataType, targetDomain);
        
        try {
            String url = aiAgentUrl + "/extract";
            
            String jsonBody = String.format("{\"targetDomain\": \"%s\", \"dataType\": \"%s\"}", targetDomain, dataType);
            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
            
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().string();
                } else {
                    logger.warn("Extraction failed with code: {}", response.code());
                    return "{}";
                }
            }
            
        } catch (IOException e) {
            logger.error("Error extracting data", e);
            return "{}";
        }
    }

    /**
     * Get GraphQL schema from target endpoint
     * @param graphqlEndpoint The GraphQL endpoint URL
     * @return GraphQL schema string
     */
    public String getGraphQLSchema(String graphqlEndpoint) {
        logger.info("Fetching GraphQL schema from: {}", graphqlEndpoint);
        
        String introspectionQuery = "{ __schema { types { name fields { name type { name kind } } } } }";
        
        try {
            String jsonBody = String.format("{\"query\": \"%s\"}", introspectionQuery.replace("\"", "\\\""));
            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
            
            Request request = new Request.Builder()
                    .url(graphqlEndpoint)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().string();
                } else {
                    logger.warn("GraphQL schema fetch failed with code: {}", response.code());
                    return "{}";
                }
            }
            
        } catch (IOException e) {
            logger.error("Error fetching GraphQL schema", e);
            return "{}";
        }
    }

    private List<String> parseEndpointsFromResponse(String responseBody) {
        // Simple parsing - in production use Jackson ObjectMapper
        // Expected format: {"endpoints": ["/api/users", "/api/products", ...]}
        try {
            int start = responseBody.indexOf("[");
            int end = responseBody.lastIndexOf("]");
            if (start >= 0 && end > start) {
                String arrayContent = responseBody.substring(start + 1, end);
                String[] endpoints = arrayContent.split(",");
                java.util.ArrayList<String> result = new java.util.ArrayList<>();
                for (String ep : endpoints) {
                    String cleaned = ep.trim().replace("\"", "").replace("'", "");
                    if (!cleaned.isEmpty()) {
                        result.add(cleaned);
                    }
                }
                return result;
            }
        } catch (Exception e) {
            logger.warn("Error parsing endpoints", e);
        }
        return Collections.emptyList();
    }
}
