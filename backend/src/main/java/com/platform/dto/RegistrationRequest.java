package com.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationRequest {

    @NotBlank(message = "Public IP is required")
    private String publicIp;

    private String privateIp;

    private String region;

    @NotNull(message = "Status is required")
    private DeviceStatus status;

    private Integer currentLoad;

    public enum DeviceStatus {
        IDLE, BUSY, OFFLINE
    }
}
