package com.platform.service;

import com.platform.dto.RegistrationRequest;
import com.platform.entity.Device;
import com.platform.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);

    @Autowired
    private DeviceRepository deviceRepository;

    public Device registerDevice(RegistrationRequest request) {
        log.info("Registering device with IP: {}", request.getPublicIp());

        Optional<Device> existingDevice = deviceRepository.findByPublicIp(request.getPublicIp());
        if (existingDevice.isPresent()) {
            log.info("Device already exists, updating...");
            Device device = existingDevice.get();
            device.setPrivateIp(request.getPrivateIp());
            device.setRegion(request.getRegion());
            device.setStatus(convertStatus(request.getStatus()));
            device.setCurrentLoad(request.getCurrentLoad() != null ? request.getCurrentLoad() : 0);
            device.setLastHeartbeat(Instant.now());
            return deviceRepository.save(device);
        }

        Device device = Device.builder()
                .publicIp(request.getPublicIp())
                .privateIp(request.getPrivateIp())
                .region(request.getRegion())
                .status(convertStatus(request.getStatus()))
                .currentLoad(request.getCurrentLoad() != null ? request.getCurrentLoad() : 0)
                .lastHeartbeat(Instant.now())
                .build();

        return deviceRepository.save(device);
    }

    public Device updateHeartbeat(UUID deviceId, Integer currentLoad, Device.Status status) {
        log.debug("Updating heartbeat for device: {}", deviceId);

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found: " + deviceId));

        device.setLastHeartbeat(Instant.now());
        device.setCurrentLoad(currentLoad != null ? currentLoad : device.getCurrentLoad());
        device.setStatus(status != null ? status : device.getStatus());

        return deviceRepository.save(device);
    }

    @Transactional(readOnly = true)
    public List<Device> getAvailableDevices() {
        log.debug("Fetching available devices");
        return deviceRepository.findAvailableDevicesOrderedByLoad();
    }

    @Transactional(readOnly = true)
    public List<Device> getAvailableDevicesByRegion(String region) {
        log.debug("Fetching available devices in region: {}", region);
        return deviceRepository.findAvailableDevicesByRegion(region);
    }

    public Device getLeastLoadedDevice() {
        List<Device> availableDevices = getAvailableDevices();
        if (availableDevices.isEmpty()) {
            throw new RuntimeException("No available devices");
        }
        return availableDevices.get(0);
    }

    @Transactional(readOnly = true)
    public Optional<Device> findById(UUID id) {
        return deviceRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public long countByStatus(Device.Status status) {
        return deviceRepository.countByStatus(status);
    }

    private Device.Status convertStatus(RegistrationRequest.DeviceStatus status) {
        if (status == null) {
            return Device.Status.IDLE;
        }
        return Device.Status.valueOf(status.name());
    }
}
