package com.platform.repository;

import com.platform.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    
    Optional<Device> findByPublicIp(String publicIp);
    
    List<Device> findByStatus(Device.Status status);
    
    @Query("SELECT d FROM Device d WHERE d.status = 'IDLE' ORDER BY d.currentLoad ASC")
    List<Device> findAvailableDevicesOrderedByLoad();
    
    @Query("SELECT COUNT(d) FROM Device d WHERE d.status = 'IDLE'")
    long countActiveDevices();
    
    @Query("SELECT d FROM Device d WHERE d.status = 'IDLE' AND d.region = :region ORDER BY d.currentLoad ASC")
    List<Device> findAvailableDevicesByRegion(String region);
}
