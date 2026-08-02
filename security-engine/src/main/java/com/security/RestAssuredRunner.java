package com.security;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * REST Assured runner for executing HTTP requests during security tests.
 * Supports GET, POST, PUT, DELETE with dynamic parameters.
 */
public class RestAssuredRunner {

    private static final Logger logger = LoggerFactory.getLogger(RestAssuredRunner.class);
    
    public RestAssuredRunner() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    /**
     * Execute a GET request.
     * @param url Target URL
     * @param headers Request headers
     * @return Response object
     */
    public Response executeGet(String url, Map<String, String> headers) {
        logger.info("Executing GET: {}", url);
        
        try {
            var spec = given().headers(headers != null ? headers : new HashMap<>());
            return spec.when().get(url);
        } catch (Exception e) {
            logger.error("GET request failed", e);
            return null;
        }
    }

    /**
     * Execute a POST request with JSON body.
     * @param url Target URL
     * @param body Request body as string
     * @param headers Request headers
     * @return Response object
     */
    public Response executePost(String url, String body, Map<String, String> headers) {
        logger.info("Executing POST: {}", url);
        
        try {
            var spec = given()
                    .headers(headers != null ? headers : new HashMap<>())
                    .contentType("application/json")
                    .body(body);
            return spec.when().post(url);
        } catch (Exception e) {
            logger.error("POST request failed", e);
            return null;
        }
    }

    /**
     * Execute a PUT request with JSON body.
     * @param url Target URL
     * @param body Request body as string
     * @param headers Request headers
     * @return Response object
     */
    public Response executePut(String url, String body, Map<String, String> headers) {
        logger.info("Executing PUT: {}", url);
        
        try {
            var spec = given()
                    .headers(headers != null ? headers : new HashMap<>())
                    .contentType("application/json")
                    .body(body);
            return spec.when().put(url);
        } catch (Exception e) {
            logger.error("PUT request failed", e);
            return null;
        }
    }

    /**
     * Execute a DELETE request.
     * @param url Target URL
     * @param headers Request headers
     * @return Response object
     */
    public Response executeDelete(String url, Map<String, String> headers) {
        logger.info("Executing DELETE: {}", url);
        
        try {
            var spec = given().headers(headers != null ? headers : new HashMap<>());
            return spec.when().delete(url);
        } catch (Exception e) {
            logger.error("DELETE request failed", e);
            return null;
        }
    }

    /**
     * Execute a request with path parameters.
     * @param url Target URL with path placeholders (e.g., /api/users/{id})
     * @param method HTTP method
     * @param body Request body (for POST/PUT)
     * @param pathParams Path parameters map
     * @param headers Request headers
     * @return Response object
     */
    public Response executeWithParams(String url, String method, String body, 
                                       Map<String, String> pathParams, 
                                       Map<String, String> headers) {
        logger.info("Executing {} with params: {}", method, url);
        
        try {
            var spec = given()
                    .headers(headers != null ? headers : new HashMap<>())
                    .pathParams(pathParams != null ? pathParams : new HashMap<>());
            
            if (body != null && !body.isEmpty()) {
                spec.contentType("application/json").body(body);
            }
            
            return spec.when().request(method.toUpperCase(), url);
        } catch (Exception e) {
            logger.error("Request with params failed", e);
            return null;
        }
    }

    /**
     * Test endpoint with different authentication tokens.
     * @param url Target URL
     * @param method HTTP method
     * @param tokens Array of auth tokens to test
     * @return Test results map
     */
    public Map<String, Integer> testAuthTokens(String url, String method, String[] tokens) {
        Map<String, Integer> results = new HashMap<>();
        
        for (String token : tokens) {
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + token);
            
            Response response = executeWithParams(url, method, null, null, headers);
            int statusCode = response != null ? response.getStatusCode() : 0;
            
            results.put(token.substring(0, Math.min(20, token.length())), statusCode);
            logger.info("Token test result: {} -> {}", token.substring(0, 10), statusCode);
        }
        
        return results;
    }
}
