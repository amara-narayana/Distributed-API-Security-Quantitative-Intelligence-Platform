package com.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestExecutionRequest {

    private String targetUrl;

    private List<String> testTypes;

    private String depth;

    private Boolean includeGraphQL;

    private Map<String, Object> parameters;
}
