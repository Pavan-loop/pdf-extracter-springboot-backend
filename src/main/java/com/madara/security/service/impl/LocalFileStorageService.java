package com.madara.security.service.impl;

import com.madara.security.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@ConditionalOnMissingBean(name = "s3FileStorageService")
@Primary
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    @Value("${application.file.upload-dir:./uploads/}")
    private String uploadDir;

    @Override
    public String store(String jobId, String originalFilename, MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);
        Path filePath = uploadPath.resolve(jobId + "_" + originalFilename);
        file.transferTo(filePath.toFile());
        log.info("File stored locally: {}", filePath);
        return filePath.toAbsolutePath().toString();
    }

    @Override
    public void delete(String filePath) {
        try {
            boolean deleted = Files.deleteIfExists(Paths.get(filePath));
            if (deleted) log.info("File deleted from disk: {}", filePath);
            else log.warn("File not found on disk (already deleted?): {}", filePath);
        } catch (IOException e) {
            log.error("Failed to delete file from disk: {} — {}", filePath, e.getMessage());
        }
    }
}
