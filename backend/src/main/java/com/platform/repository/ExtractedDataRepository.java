package com.platform.repository;

import com.platform.entity.ExtractedData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExtractedDataRepository extends JpaRepository<ExtractedData, UUID> {
    
    List<ExtractedData> findBySourceDomain(String sourceDomain);
    
    List<ExtractedData> findByProductId(String productId);
    
    List<ExtractedData> findByProductNameContaining(String productName);
}
