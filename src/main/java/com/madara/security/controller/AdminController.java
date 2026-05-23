package com.madara.security.controller;

import com.madara.security.model.Role;
import com.madara.security.response.DTO.ApiResponse;
import com.madara.security.response.DTO.PageResponse;
import com.madara.security.response.DTO.admin.AdminPdfDTO;
import com.madara.security.response.DTO.admin.AdminStatsDTO;
import com.madara.security.response.DTO.admin.AdminUserDetailDTO;
import com.madara.security.response.DTO.admin.AdminUserSummaryDTO;
import com.madara.security.response.DTO.admin.UpdatePlanRequest;
import com.madara.security.security.authentication.AuthenticationService;
import com.madara.security.security.authentication.RegistrationRequest;
import com.madara.security.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("admin")
public class AdminController {

    private final AdminService adminService;
    private final AuthenticationService authenticationService;

    /**
     * Create a new admin account. Requires an existing admin JWT.
     * POST /admin/create-admin
     */
    @PostMapping("/create-admin")
    public ResponseEntity<ApiResponse<Void>> createAdmin(
            @RequestBody @Valid RegistrationRequest request
    ) {
        authenticationService.registerUser(request, Role.ADMIN);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .status(HttpStatus.CREATED)
                .message("Admin account created. Please verify the email with the OTP sent")
                .data(null)
                .build();
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Platform-wide overview: user counts, PDF totals, success rate, growth.
     * GET /admin/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsDTO>> getPlatformStats() {
        AdminStatsDTO stats = adminService.getPlatformStats();
        return ResponseEntity.ok(ApiResponse.success(stats, "Platform stats fetched", HttpStatus.OK));
    }

    /**
     * Paginated list of all users with their session and PDF stats.
     * GET /admin/users?page=0&size=20&sortBy=createdAt&sortDir=desc
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserSummaryDTO>>> getAllUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        PageResponse<AdminUserSummaryDTO> result = adminService.getAllUsers(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(result, "Users fetched", HttpStatus.OK));
    }

    /**
     * Full detail for a single user: profile, stats, and last 10 PDF jobs.
     * GET /admin/users/{userId}
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<AdminUserDetailDTO>> getUserDetail(
            @PathVariable Long userId
    ) {
        AdminUserDetailDTO result = adminService.getUserDetail(userId);
        return ResponseEntity.ok(ApiResponse.success(result, "User detail fetched", HttpStatus.OK));
    }

    /**
     * Paginated PDF jobs across all users. Filter by status: ALL | DONE | FAILED | PENDING.
     * GET /admin/pdfs?page=0&size=20&status=ALL
     */
    @GetMapping("/pdfs")
    public ResponseEntity<ApiResponse<PageResponse<AdminPdfDTO>>> getAllPdfs(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size,
            @RequestParam(defaultValue = "ALL") String status
    ) {
        PageResponse<AdminPdfDTO> result = adminService.getAllPdfs(page, size, status);
        return ResponseEntity.ok(ApiResponse.success(result, "PDFs fetched", HttpStatus.OK));
    }

    /**
     * Live feed — last 20 PDF jobs across all users, newest first.
     * GET /admin/activity
     */
    @GetMapping("/activity")
    public ResponseEntity<ApiResponse<List<AdminPdfDTO>>> getRecentActivity() {
        List<AdminPdfDTO> result = adminService.getRecentActivity();
        return ResponseEntity.ok(ApiResponse.success(result, "Recent activity fetched", HttpStatus.OK));
    }

    /**
     * Update a user's plan. Use this to upgrade to SUBSCRIBER or assign DEV plan.
     * PATCH /admin/users/{userId}/plan
     * Body: { "plan": "SUBSCRIBER", "subscriptionDays": 30 }
     */
    @PatchMapping("/users/{userId}/plan")
    public ResponseEntity<ApiResponse<Void>> updateUserPlan(
            @PathVariable Long userId,
            @RequestBody @Valid UpdatePlanRequest request
    ) {
        adminService.updateUserPlan(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null,
                "User plan updated to " + request.getPlan().name(), HttpStatus.OK));
    }
}
