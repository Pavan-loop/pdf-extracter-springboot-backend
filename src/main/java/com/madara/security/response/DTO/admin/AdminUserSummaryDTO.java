package com.madara.security.response.DTO.admin;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Set;

@Getter
@Builder
public class AdminUserSummaryDTO {
    private Long id;
    private String name;
    private String email;
    private Set<String> roles;
    private String authProvider;
    private boolean accountEnabled;
    private Instant createdAt;

    // Plan
    private String plan;
    private boolean unlimited;
    private int pagesUploadedThisMonth;
    private int monthlyPageLimit;
    private String subscriptionExpiresAt;

    // Stats
    private long totalSessions;
    private long totalPdfs;
    private long successPdfs;
    private long failedPdfs;
}
