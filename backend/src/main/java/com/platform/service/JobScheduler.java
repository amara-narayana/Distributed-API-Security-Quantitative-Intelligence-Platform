package com.platform.service;

import com.platform.entity.Device;
import com.platform.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class JobScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobScheduler.class);
    private static final long JOB_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(30);
    private static final int MAX_RETRIES = 3;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private DeviceService deviceService;

    private final ConcurrentHashMap<UUID, JobContext> activeJobs = new ConcurrentHashMap<>();

    public void assignJob(UUID jobId, String jobType, Object payload, String targetRegion) {
        log.info("Assigning job {} of type {}", jobId, jobType);

        try {
            Device device;
            if (targetRegion != null) {
                List<Device> devices = deviceService.getAvailableDevicesByRegion(targetRegion);
                if (devices.isEmpty()) {
                    device = deviceService.getLeastLoadedDevice();
                } else {
                    device = devices.get(0);
                }
            } else {
                device = deviceService.getLeastLoadedDevice();
            }

            device.setStatus(Device.Status.BUSY);
            device.setCurrentLoad(device.getCurrentLoad() + 1);
            deviceRepository.save(device);

            JobContext context = new JobContext(jobId, jobType, payload, device.getId(), Instant.now());
            activeJobs.put(jobId, context);

            log.info("Job {} assigned to device {}", jobId, device.getId());

        } catch (Exception e) {
            log.error("Failed to assign job {}: {}", jobId, e.getMessage());
            throw new RuntimeException("Job assignment failed", e);
        }
    }

    public void completeJob(UUID jobId, boolean success, String errorMessage) {
        log.info("Completing job {} with success={}", jobId, success);

        JobContext context = activeJobs.remove(jobId);
        if (context == null) {
            log.warn("Job {} not found in active jobs", jobId);
            return;
        }

        try {
            Device device = deviceRepository.findById(context.getDeviceId())
                    .orElseThrow(() -> new RuntimeException("Device not found"));

            device.setCurrentLoad(Math.max(0, device.getCurrentLoad() - 1));
            if (device.getCurrentLoad() == 0) {
                device.setStatus(Device.Status.IDLE);
            }
            deviceRepository.save(device);

            log.info("Device {} load updated after job completion", device.getId());

        } catch (Exception e) {
            log.error("Error completing job {}: {}", jobId, e.getMessage());
        }
    }

    public void retryJob(UUID jobId, String jobType, Object payload) {
        JobContext context = activeJobs.get(jobId);
        if (context == null) {
            log.warn("Cannot retry job {}: not found", jobId);
            return;
        }

        if (context.getRetryCount() >= MAX_RETRIES) {
            log.error("Job {} exceeded max retries, marking as failed", jobId);
            completeJob(jobId, false, "Max retries exceeded");
            return;
        }

        context.incrementRetry();
        log.info("Retrying job {} (attempt {}/{})", jobId, context.getRetryCount(), MAX_RETRIES);

        try {
            assignJob(jobId, jobType, payload, null);
        } catch (Exception e) {
            log.error("Retry failed for job {}: {}", jobId, e.getMessage());
        }
    }

    @Scheduled(fixedRate = 60000)
    public void checkTimeouts() {
        log.debug("Checking for timed out jobs");
        Instant now = Instant.now();

        activeJobs.entrySet().stream()
                .filter(entry -> Duration.between(entry.getValue().getStartedAt(), now).toMillis() > JOB_TIMEOUT_MS)
                .forEach(entry -> {
                    UUID jobId = entry.getKey();
                    JobContext context = entry.getValue();
                    log.warn("Job {} timed out", jobId);
                    retryJob(jobId, context.getJobType(), context.getPayload());
                });
    }

    private static class JobContext {
        private final UUID jobId;
        private final String jobType;
        private final Object payload;
        private final UUID deviceId;
        private final Instant startedAt;
        private int retryCount;

        public JobContext(UUID jobId, String jobType, Object payload, UUID deviceId, Instant startedAt) {
            this.jobId = jobId;
            this.jobType = jobType;
            this.payload = payload;
            this.deviceId = deviceId;
            this.startedAt = startedAt;
            this.retryCount = 0;
        }

        public void incrementRetry() {
            this.retryCount++;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public UUID getJobId() {
            return jobId;
        }

        public String getJobType() {
            return jobType;
        }

        public Object getPayload() {
            return payload;
        }

        public UUID getDeviceId() {
            return deviceId;
        }

        public Instant getStartedAt() {
            return startedAt;
        }
    }
}
