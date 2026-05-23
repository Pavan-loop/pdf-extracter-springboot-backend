package com.madara.security.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {

    /**
     * Store the file and return the storage path or S3 key.
     * This path is persisted in the DB and used for deletion later.
     */
    String store(String jobId, String originalFilename, MultipartFile file) throws IOException;

    /**
     * Delete the file using the path/key returned by store().
     */
    void delete(String filePathOrKey);
}
