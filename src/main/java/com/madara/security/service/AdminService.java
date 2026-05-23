package com.madara.security.service;

import com.madara.security.response.DTO.PageResponse;
import com.madara.security.response.DTO.admin.AdminPdfDTO;
import com.madara.security.response.DTO.admin.AdminStatsDTO;
import com.madara.security.response.DTO.admin.AdminUserDetailDTO;
import com.madara.security.response.DTO.admin.AdminUserSummaryDTO;
import com.madara.security.response.DTO.admin.UpdatePlanRequest;

import java.util.List;

public interface AdminService {

    AdminStatsDTO getPlatformStats();

    PageResponse<AdminUserSummaryDTO> getAllUsers(int page, int size, String sortBy, String sortDir);

    AdminUserDetailDTO getUserDetail(Long userId);

    PageResponse<AdminPdfDTO> getAllPdfs(int page, int size, String status);

    List<AdminPdfDTO> getRecentActivity();

    void updateUserPlan(Long userId, UpdatePlanRequest request);
}
