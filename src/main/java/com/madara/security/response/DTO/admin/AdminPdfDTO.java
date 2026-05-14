package com.madara.security.response.DTO.admin;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AdminPdfDTO {
    private Long id;
    private String jobId;
    private Long userId;
    private String userEmail;
    private String userName;
    private String filename;
    private String documentType;
    private String status;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;
}
