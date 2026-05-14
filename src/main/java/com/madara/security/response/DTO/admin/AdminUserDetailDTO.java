package com.madara.security.response.DTO.admin;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Getter
@Builder
public class AdminUserDetailDTO {
    private Long id;
    private String name;
    private String username;
    private String email;
    private Long phoneNumber;
    private Set<String> roles;
    private String authProvider;
    private boolean accountEnabled;
    private Instant createdAt;
    private Instant modifiedAt;

    // Stats
    private long totalSessions;
    private long totalPdfs;
    private long successPdfs;
    private long failedPdfs;
    private long pendingPdfs;

    // Recent activity
    private List<AdminPdfDTO> recentPdfs;
}
