package com.platform.repository;

import com.platform.entity.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, UUID> {
    
    List<TestResult> findByTestType(String testType);
    
    List<TestResult> findByVulnerabilityFoundTrue();
    
    List<TestResult> findBySeverity(TestResult.Severity severity);
    
    @Query("SELECT tr FROM TestResult tr WHERE tr.vulnerabilityFound = true ORDER BY tr.timestamp DESC")
    List<TestResult> findRecentVulnerabilities();
}
