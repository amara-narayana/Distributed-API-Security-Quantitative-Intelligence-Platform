package com.platform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.UUID;

@Service
public class S3Service {

    private static final Logger log = LoggerFactory.getLogger(S3Service.class);

    @Value("${aws.access-key-id:}")
    private String accessKeyId;

    @Value("${aws.secret-access-key:}")
    private String secretAccessKey;

    @Value("${aws.s3.bucket:security-platform-bucket}")
    private String bucketName;

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    private S3Client s3Client;
    private S3Presigner presigner;

    public S3Service() {
        initializeClients();
    }

    private void initializeClients() {
        if (accessKeyId != null && !accessKeyId.isEmpty() && 
            secretAccessKey != null && !secretAccessKey.isEmpty()) {
            
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
            
            this.s3Client = S3Client.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .region(Region.of(region))
                    .build();

            this.presigner = S3Presigner.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .region(Region.of(region))
                    .build();

            log.info("S3 client initialized for bucket: {} in region: {}", bucketName, region);
        } else {
            log.warn("AWS credentials not configured. S3 operations will be skipped.");
        }
    }

    public String uploadPayload(String data, String contentType) {
        if (s3Client == null) {
            log.warn("S3 client not initialized, skipping upload");
            return null;
        }

        try {
            String key = "payloads/" + UUID.randomUUID() + ".json";
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, 
                    software.amazon.awssdk.core.sync.RequestBody.fromInputStream(
                            new ByteArrayInputStream(data.getBytes()), 
                            data.length()));

            log.info("Uploaded payload to S3: s3://{}/{}", bucketName, key);
            return key;

        } catch (Exception e) {
            log.error("Failed to upload to S3: {}", e.getMessage());
            throw new RuntimeException("S3 upload failed", e);
        }
    }

    public String getPreSignedUrl(String objectKey, Duration expiration) {
        if (presigner == null) {
            log.warn("Presigner not initialized, cannot generate pre-signed URL");
            return null;
        }

        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(builder -> builder.bucket(bucketName).key(objectKey))
                    .build();

            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
            String url = presignedRequest.url().toString();

            log.info("Generated pre-signed URL for key: {}, expires in: {}", objectKey, expiration);
            return url;

        } catch (Exception e) {
            log.error("Failed to generate pre-signed URL: {}", e.getMessage());
            throw new RuntimeException("Pre-signed URL generation failed", e);
        }
    }

    public String getPreSignedUrl(String objectKey) {
        return getPreSignedUrl(objectKey, Duration.ofHours(1));
    }

    public boolean deleteObject(String objectKey) {
        if (s3Client == null) {
            return false;
        }

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("Deleted object from S3: s3://{}/{}", bucketName, objectKey);
            return true;

        } catch (Exception e) {
            log.error("Failed to delete from S3: {}", e.getMessage());
            return false;
        }
    }

    public void close() {
        if (s3Client != null) {
            s3Client.close();
        }
        if (presigner != null) {
            presigner.close();
        }
    }
}
