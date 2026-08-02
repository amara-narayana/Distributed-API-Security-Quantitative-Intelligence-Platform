package com.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobAssignment {

    private UUID jobId;

    private String jobType;

    private Object payload;

    private UUID assignedDeviceId;

    private String status;
}
