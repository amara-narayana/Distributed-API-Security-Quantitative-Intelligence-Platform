package com.platform.repository;

import com.platform.entity.ApiRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApiRequestRepository extends JpaRepository<ApiRequest, UUID> {
    
    List<ApiRequest> findByDeviceId(UUID deviceId);
    
    List<ApiRequest> findByTargetUrlContaining(String targetUrl);
    
    List<ApiRequest> findByStatusCodeBetween(int minCode, int maxCode);
}
