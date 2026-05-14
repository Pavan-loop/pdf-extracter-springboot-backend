package com.madara.security.response.DTO.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminStatsDTO {

    // ── User metrics ──────────────────────────────────────────
    private long totalUsers;
    private long verifiedUsers;
    private long unverifiedUsers;
    private long localUsers;
    private long googleUsers;
    private long newUsersToday;
    private long newUsersThisMonth;

    // ── PDF metrics ───────────────────────────────────────────
    private long totalPdfs;
    private long successPdfs;
    private long failedPdfs;
    private long pendingPdfs;
    private double successRate;
    private long pdfsToday;
    private long pdfsThisWeek;
    private long pdfsThisMonth;

    // ── Session metrics ───────────────────────────────────────
    private long totalSessions;
}
