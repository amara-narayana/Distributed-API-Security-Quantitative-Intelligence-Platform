package com.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuantAnalysisRequest {

    private List<String> symbols;

    private String analysisType;

    private String timeframe;

    private Boolean includeBacktest;
}
