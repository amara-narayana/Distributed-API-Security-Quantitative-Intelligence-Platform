package com.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main orchestrator for security testing operations.
 * Coordinates endpoint discovery, test generation, execution, and result reporting.
 */
public class SecurityTestOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(SecurityTestOrchestrator.class);
    
    private final EndpointDiscoveryClient discoveryClient;
    private final IdorTestGenerator idorGenerator;
    private final GraphQLTestClient graphqlClient;
    private final RestAssuredRunner restRunner;
    private final ResultAnalyzer resultAnalyzer;
    private final ExecutorService executor;
    
    private String backendUrl;

    public SecurityTestOrchestrator(String aiAgentUrl, String backendUrl) {
        this.discoveryClient = new EndpointDiscoveryClient(aiAgentUrl);
        this.idorGenerator = new IdorTestGenerator();
        this.graphqlClient = new GraphQLTestClient();
        this.restRunner = new RestAssuredRunner();
        this.resultAnalyzer = new ResultAnalyzer();
        this.backendUrl = backendUrl;
        this.executor = Executors.newFixedThreadPool(10);
    }

    /**
     * Run a complete security test suite on a target domain.
     * @param targetDomain Target domain to test
     * @param testTypes Types of tests to run (IDOR, BOLA, GRAPHQL)
     * @return Test results
     */
    public TestResults runSecurityTests(String targetDomain, List<String> testTypes) {
        logger.info("Starting security tests on domain: {}", targetDomain);
        
        TestResults results = new TestResults();
        results.setTargetDomain(targetDomain);
        results.setTestTypes(testTypes);
        
        try {
            // Step 1: Discover endpoints
            logger.info("Phase 1: Discovering endpoints...");
            List<String> endpoints = discoveryClient.discoverEndpoints(targetDomain);
            results.setDiscoveredEndpoints(endpoints);
            logger.info("Discovered {} endpoints", endpoints.size());
            
            // Step 2: Run requested tests
            if (testTypes.contains("IDOR") || testTypes.contains("BOLA")) {
                logger.info("Phase 2: Running IDOR/BOLA tests...");
                List<ResultAnalyzer.AnalysisResult> idorResults = runIdorTests(endpoints);
                results.setIdorResults(idorResults);
            }
            
            if (testTypes.contains("GRAPHQL")) {
                logger.info("Phase 3: Running GraphQL tests...");
                List<GraphQLTestClient.GraphQLVulnerability> graphqlResults = runGraphqlTests(targetDomain);
                results.setGraphqlResults(graphqlResults);
            }
            
            // Step 4: Compile summary
            results.setVulnerabilitiesFound(countVulnerabilities(results));
            results.setCompleted(true);
            
        } catch (Exception e) {
            logger.error("Security test failed", e);
            results.setError(e.getMessage());
            results.setCompleted(false);
        }
        
        return results;
    }

    /**
     * Run IDOR tests on discovered endpoints.
     */
    private List<ResultAnalyzer.AnalysisResult> runIdorTests(List<String> endpoints) {
        List<ResultAnalyzer.AnalysisResult> allResults = new ArrayList<>();
        
        for (String endpoint : endpoints) {
            if (endpoint.contains("{") || endpoint.contains(":")) {
                // Parameterized endpoint, good candidate for IDOR testing
                List<ResultAnalyzer.AnalysisResult> endpointResults = 
                    testEndpointForIdor(endpoint);
                allResults.addAll(endpointResults);
            }
        }
        
        return allResults;
    }

    /**
     * Test a single endpoint for IDOR vulnerabilities.
     */
    private List<ResultAnalyzer.AnalysisResult> testEndpointForIdor(String endpoint) {
        List<ResultAnalyzer.AnalysisResult> results = new ArrayList<>();
        
        // Extract parameter name from endpoint (e.g., /api/users/{id} -> id)
        String paramName = extractParamName(endpoint);
        if (paramName == null) {
            return results;
        }
        
        // Generate test payloads
        String originalId = "123"; // In production, get from initial request
        List<IdorTestGenerator.TestPayload> payloads = 
            idorGenerator.generateTestPayloads(endpoint, paramName, originalId);
        
        // Execute tests in parallel
        List<CompletableFuture<ResultAnalyzer.AnalysisResult>> futures = new ArrayList<>();
        
        for (IdorTestGenerator.TestPayload payload : payloads) {
            CompletableFuture<ResultAnalyzer.AnalysisResult> future = 
                CompletableFuture.supplyAsync(() -> executeIdorTest(payload), executor);
            futures.add(future);
        }
        
        // Collect results
        for (CompletableFuture<ResultAnalyzer.AnalysisResult> future : futures) {
            try {
                ResultAnalyzer.AnalysisResult result = future.join();
                if (result != null) {
                    results.add(result);
                }
            } catch (Exception e) {
                logger.warn("Test execution failed", e);
            }
        }
        
        return results;
    }

    /**
     * Execute a single IDOR test.
     */
    private ResultAnalyzer.AnalysisResult executeIdorTest(IdorTestGenerator.TestPayload payload) {
        try {
            Map<String, String> pathParams = new HashMap<>();
            pathParams.put(payload.getParamName(), payload.getTestValue());
            
            var response = restRunner.executeWithParams(
                payload.getEndpoint(), 
                "GET", 
                null, 
                pathParams, 
                new HashMap<>()
            );
            
            if (response != null) {
                String responseBody = response.getBody().asString();
                int statusCode = response.getStatusCode();
                
                return resultAnalyzer.analyzeResponse(
                    statusCode, 
                    responseBody, 
                    payload.getOriginalValue(), 
                    payload.getTestValue()
                );
            }
        } catch (Exception e) {
            logger.error("IDOR test execution failed for payload: {}", payload, e);
        }
        
        return null;
    }

    /**
     * Run GraphQL security tests.
     */
    private List<GraphQLTestClient.GraphQLVulnerability> runGraphqlTests(String targetDomain) {
        List<GraphQLTestClient.GraphQLVulnerability> vulnerabilities = new ArrayList<>();
        
        // Try common GraphQL endpoints
        String[] graphqlPaths = {"/graphql", "/graph", "/api/graphql", "/v1/graphql"};
        
        for (String path : graphqlPaths) {
            String graphqlEndpoint = "https://" + targetDomain + path;
            logger.debug("Testing GraphQL endpoint: {}", graphqlEndpoint);
            
            List<GraphQLTestClient.GraphQLVulnerability> endpointVulns = 
                graphqlClient.testVulnerabilities(graphqlEndpoint);
            
            if (!endpointVulns.isEmpty()) {
                vulnerabilities.addAll(endpointVulns);
            }
        }
        
        return vulnerabilities;
    }

    /**
     * Extract parameter name from endpoint path.
     */
    private String extractParamName(String endpoint) {
        if (endpoint.contains("{")) {
            int start = endpoint.indexOf("{") + 1;
            int end = endpoint.indexOf("}");
            if (start > 0 && end > start) {
                return endpoint.substring(start, end);
            }
        } else if (endpoint.contains(":")) {
            // Express-style params: /api/users/:id
            String[] parts = endpoint.split("/");
            for (String part : parts) {
                if (part.startsWith(":")) {
                    return part.substring(1);
                }
            }
        }
        return null;
    }

    /**
     * Count total vulnerabilities found.
     */
    private int countVulnerabilities(TestResults results) {
        int count = 0;
        
        if (results.getIdorResults() != null) {
            for (ResultAnalyzer.AnalysisResult result : results.getIdorResults()) {
                if (result.isVulnerabilityFound()) {
                    count++;
                }
            }
        }
        
        if (results.getGraphqlResults() != null) {
            count += results.getGraphqlResults().size();
        }
        
        return count;
    }

    /**
     * Submit test results to the backend.
     */
    public void submitResults(TestResults results) {
        logger.info("Submitting test results to backend...");
        
        try {
            // In production, use HTTP client to POST results to backend
            // For now, just log
            logger.info("Results would be submitted to: {}/api/security/results", backendUrl);
            logger.info("Vulnerabilities found: {}", results.getVulnerabilitiesFound());
        } catch (Exception e) {
            logger.error("Failed to submit results", e);
        }
    }

    /**
     * Inner class representing complete test results.
     */
    public static class TestResults {
        private String targetDomain;
        private List<String> testTypes;
        private List<String> discoveredEndpoints;
        private List<ResultAnalyzer.AnalysisResult> idorResults;
        private List<GraphQLTestClient.GraphQLVulnerability> graphqlResults;
        private int vulnerabilitiesFound;
        private boolean completed;
        private String error;

        public String getTargetDomain() { return targetDomain; }
        public void setTargetDomain(String domain) { this.targetDomain = domain; }
        
        public List<String> getTestTypes() { return testTypes; }
        public void setTestTypes(List<String> types) { this.testTypes = types; }
        
        public List<String> getDiscoveredEndpoints() { return discoveredEndpoints; }
        public void setDiscoveredEndpoints(List<String> endpoints) { this.discoveredEndpoints = endpoints; }
        
        public List<ResultAnalyzer.AnalysisResult> getIdorResults() { return idorResults; }
        public void setIdorResults(List<ResultAnalyzer.AnalysisResult> results) { this.idorResults = results; }
        
        public List<GraphQLTestClient.GraphQLVulnerability> getGraphqlResults() { return graphqlResults; }
        public void setGraphqlResults(List<GraphQLTestClient.GraphQLVulnerability> results) { this.graphqlResults = results; }
        
        public int getVulnerabilitiesFound() { return vulnerabilitiesFound; }
        public void setVulnerabilitiesFound(int count) { this.vulnerabilitiesFound = count; }
        
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    /**
     * Main entry point for standalone execution.
     */
    public static void main(String[] args) {
        logger.info("Security Test Orchestrator starting...");
        
        String aiAgentUrl = System.getenv("AI_AGENT_URL");
        String backendUrl = System.getenv("BACKEND_URL");
        
        if (aiAgentUrl == null) aiAgentUrl = "http://localhost:8001";
        if (backendUrl == null) backendUrl = "http://localhost:8080";
        
        SecurityTestOrchestrator orchestrator = new SecurityTestOrchestrator(aiAgentUrl, backendUrl);
        
        List<String> testTypes = List.of("IDOR", "BOLA", "GRAPHQL");
        TestResults results = orchestrator.runSecurityTests("example.com", testTypes);
        
        logger.info("Test completed. Vulnerabilities found: {}", results.getVulnerabilitiesFound());
        orchestrator.submitResults(results);
    }
}
