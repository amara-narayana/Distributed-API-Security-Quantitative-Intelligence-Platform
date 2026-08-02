package com.platform.controller;

import com.platform.dto.RegistrationRequest;
import com.platform.entity.Device;
import com.platform.service.DeviceService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private static final Logger log = LoggerFactory.getLogger(DeviceController.class);

    @Autowired
    private DeviceService deviceService;

    @PostMapping("/register")
    public ResponseEntity<?> registerDevice(@Valid @RequestBody RegistrationRequest request) {
        log.info("Received device registration request from IP: {}", request.getPublicIp());

        try {
            Device device = deviceService.registerDevice(request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Device registered successfully",
                    "deviceId", device.getId(),
                    "status", device.getStatus()
            ));
        } catch (Exception e) {
            log.error("Failed to register device: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<?> sendHeartbeat(@RequestBody Map<String, Object> payload) {
        String deviceIdStr = (String) payload.get("deviceId");
        Integer currentLoad = (Integer) payload.get("currentLoad");
        String statusStr = (String) payload.get("status");

        try {
            UUID deviceId = UUID.fromString(deviceIdStr);
            Device.Status status = statusStr != null ? Device.Status.valueOf(statusStr) : null;

            Device device = deviceService.updateHeartbeat(deviceId, currentLoad, status);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Heartbeat received",
                    "deviceId", device.getId(),
                    "lastHeartbeat", device.getLastHeartbeat(),
                    "currentLoad", device.getCurrentLoad(),
                    "status", device.getStatus()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Invalid device ID format"
            ));
        } catch (Exception e) {
            log.error("Failed to process heartbeat: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/available")
    public ResponseEntity<?> getAvailableDevices(@RequestParam(required = false) String region) {
        try {
            List<Device> devices;
            if (region != null && !region.isEmpty()) {
                devices = deviceService.getAvailableDevicesByRegion(region);
            } else {
                devices = deviceService.getAvailableDevices();
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", devices.size(),
                    "devices", devices
            ));
        } catch (Exception e) {
            log.error("Failed to fetch available devices: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDevice(@PathVariable UUID id) {
        return deviceService.findById(id)
                .map(device -> ResponseEntity.ok(Map.of(
                        "success", true,
                        "device", device
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getDeviceStats() {
        try {
            long idleCount = deviceService.countByStatus(Device.Status.IDLE);
            long busyCount = deviceService.countByStatus(Device.Status.BUSY);
            long offlineCount = deviceService.countByStatus(Device.Status.OFFLINE);

            Map<String, Object> stats = new HashMap<>();
            stats.put("idle", idleCount);
            stats.put("busy", busyCount);
            stats.put("offline", offlineCount);
            stats.put("total", idleCount + busyCount + offlineCount);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "stats", stats
            ));
        } catch (Exception e) {
            log.error("Failed to fetch device stats: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
