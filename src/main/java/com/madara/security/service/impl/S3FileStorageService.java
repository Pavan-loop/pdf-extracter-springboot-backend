package com.madara.security.service.impl;

import com.madara.security.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

@Service("s3FileStorageService")
@Primary
@ConditionalOnProperty(name = "application.storage.s3.bucket")
@RequiredArgsConstructor
@Slf4j
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;

    @Value("${application.storage.s3.bucket}")
    private String bucket;

    @Override
    public String store(String jobId, String originalFilename, MultipartFile file) throws IOException {
        String key = "uploads/" + jobId + "_" + originalFilename;
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType())
                        .contentLength(file.getSize())
                        .build(),
                RequestBody.fromBytes(file.getBytes())
        );
        log.info("File stored in S3: bucket={} key={}", bucket, key);
        return key;
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            log.info("File deleted from S3: bucket={} key={}", bucket, key);
        } catch (Exception e) {
            log.error("Failed to delete from S3: key={} — {}", key, e.getMessage());
        }
    }
}
