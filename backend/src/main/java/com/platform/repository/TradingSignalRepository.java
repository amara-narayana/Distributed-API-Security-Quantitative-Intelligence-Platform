package com.platform.repository;

import com.platform.entity.TradingSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TradingSignalRepository extends JpaRepository<TradingSignal, UUID> {
    
    List<TradingSignal> findBySymbol(String symbol);
    
    List<TradingSignal> findByAction(TradingSignal.Action action);
    
    List<TradingSignal> findByExecutedFalse();
    
    @Query("SELECT ts FROM TradingSignal ts WHERE ts.symbol = ?1 AND ts.executed = false ORDER BY ts.generatedAt DESC")
    List<TradingSignal> findPendingSignalsBySymbol(String symbol);
}
