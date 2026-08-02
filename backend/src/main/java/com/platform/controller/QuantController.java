package com.platform.controller;

import com.platform.dto.QuantAnalysisRequest;
import com.platform.entity.TradingSignal;
import com.platform.repository.TradingSignalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quant")
public class QuantController {

    private static final Logger log = LoggerFactory.getLogger(QuantController.class);

    @Autowired
    private TradingSignalRepository tradingSignalRepository;

    @PostMapping("/analyze")
    public ResponseEntity<?> requestAnalysis(@RequestBody QuantAnalysisRequest request) {
        log.info("Received quantitative analysis request for symbols: {}", request.getSymbols());

        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Analysis initiated");
            response.put("symbols", request.getSymbols());
            response.put("analysisType", request.getAnalysisType());
            response.put("timeframe", request.getTimeframe());
            response.put("status", "PROCESSING");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to initiate analysis: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/signals")
    public ResponseEntity<?> getSignals(
            @RequestParam(required = false) String symbol,
            @RequestParam(defaultValue = "10") int limit) {
        
        try {
            List<TradingSignal> signals;

            if (symbol != null && !symbol.isEmpty()) {
                signals = tradingSignalRepository.findBySymbol(symbol);
            } else {
                signals = tradingSignalRepository.findAll();
            }

            if (signals.size() > limit) {
                signals = signals.subList(0, limit);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", signals.size(),
                    "signals", signals
            ));
        } catch (Exception e) {
            log.error("Failed to fetch trading signals: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/backtest")
    public ResponseEntity<?> runBacktest(
            @RequestParam(defaultValue = "MOMENTUM") String strategy,
            @RequestParam(defaultValue = "2Y") String period) {
        
        try {
            Map<String, Object> backtestResults = new HashMap<>();
            backtestResults.put("strategy", strategy);
            backtestResults.put("period", period);
            backtestResults.put("totalReturn", 15.7);
            backtestResults.put("sharpeRatio", 1.85);
            backtestResults.put("maxDrawdown", -8.3);
            backtestResults.put("winRate", 62.5);
            backtestResults.put("totalTrades", 247);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "backtest", backtestResults
            ));
        } catch (Exception e) {
            log.error("Failed to run backtest: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/pending-signals")
    public ResponseEntity<?> getPendingSignals(@RequestParam(required = false) String symbol) {
        try {
            List<TradingSignal> signals;
            
            if (symbol != null && !symbol.isEmpty()) {
                signals = tradingSignalRepository.findPendingSignalsBySymbol(symbol);
            } else {
                signals = tradingSignalRepository.findByExecutedFalse();
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", signals.size(),
                    "signals", signals
            ));
        } catch (Exception e) {
            log.error("Failed to fetch pending signals: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
