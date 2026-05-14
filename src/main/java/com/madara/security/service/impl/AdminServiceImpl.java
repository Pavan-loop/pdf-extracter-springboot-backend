package com.madara.security.service.impl;

import com.madara.security.Exception.type.UserNotFoundException;
import com.madara.security.model.AuthProvider;
import com.madara.security.model.PdfResult;
import com.madara.security.model.Role;
import com.madara.security.model.User;
import com.madara.security.repository.PdfResultRepository;
import com.madara.security.repository.SessionRepository;
import com.madara.security.repository.UserRepository;
import com.madara.security.response.DTO.PageResponse;
import com.madara.security.response.DTO.admin.AdminPdfDTO;
import com.madara.security.response.DTO.admin.AdminStatsDTO;
import com.madara.security.response.DTO.admin.AdminUserDetailDTO;
import com.madara.security.response.DTO.admin.AdminUserSummaryDTO;
import com.madara.security.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final PdfResultRepository pdfResultRepository;
    private final SessionRepository sessionRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "name", "email");

    @Override
    public AdminStatsDTO getPlatformStats() {
        Instant now = Instant.now();
        Instant startOfToday   = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant startOfWeek    = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant startOfMonth   = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        long totalPdfs   = pdfResultRepository.count();
        long successPdfs = pdfResultRepository.countByStatus("DONE");
        long failedPdfs  = pdfResultRepository.countByStatus("FAILED");
        long pendingPdfs = pdfResultRepository.countByStatus("PENDING");
        double successRate = totalPdfs > 0
                ? Math.round((double) successPdfs / totalPdfs * 1000.0) / 10.0
                : 0.0;

        return AdminStatsDTO.builder()
                // Users (USER role only — admins excluded)
                .totalUsers(userRepository.countByRole(Role.USER))
                .verifiedUsers(userRepository.countVerifiedUsers(Role.USER))
                .unverifiedUsers(userRepository.countUnverifiedUsers(Role.USER))
                .localUsers(userRepository.countByRoleAndAuthProvider(Role.USER, AuthProvider.LOCAL))
                .googleUsers(userRepository.countByRoleAndAuthProvider(Role.USER, AuthProvider.GOOGLE))
                .newUsersToday(userRepository.countByRoleAndCreatedAfter(Role.USER, startOfToday))
                .newUsersThisMonth(userRepository.countByRoleAndCreatedAfter(Role.USER, startOfMonth))
                // PDFs
                .totalPdfs(totalPdfs)
                .successPdfs(successPdfs)
                .failedPdfs(failedPdfs)
                .pendingPdfs(pendingPdfs)
                .successRate(successRate)
                .pdfsToday(pdfResultRepository.countByCreatedAtBetween(startOfToday, now))
                .pdfsThisWeek(pdfResultRepository.countByCreatedAtBetween(startOfWeek, now))
                .pdfsThisMonth(pdfResultRepository.countByCreatedAtBetween(startOfMonth, now))
                // Sessions
                .totalSessions(sessionRepository.count())
                .build();
    }

    @Override
    public PageResponse<AdminUserSummaryDTO> getAllUsers(int page, int size, String sortBy, String sortDir) {
        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(safeSortBy).ascending()
                : Sort.by(safeSortBy).descending();

        Page<User> userPage = userRepository.findAllByRole(Role.USER, PageRequest.of(page, size, sort));

        List<AdminUserSummaryDTO> content = userPage.getContent().stream()
                .map(this::toUserSummary)
                .collect(Collectors.toList());

        Page<AdminUserSummaryDTO> dtoPage = new PageImpl<>(content, userPage.getPageable(), userPage.getTotalElements());
        return PageResponse.from(dtoPage);
    }

    @Override
    public AdminUserDetailDTO getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        List<AdminPdfDTO> recentPdfs = pdfResultRepository
                .findTop10ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(pdf -> toPdfDto(pdf, user))
                .collect(Collectors.toList());

        return AdminUserDetailDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .roles(user.getRoles().stream().map(r -> r.name()).collect(Collectors.toSet()))
                .authProvider(user.getAuthProvider().name())
                .accountEnabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .modifiedAt(user.getModifiedAt())
                .totalSessions(sessionRepository.countByUserID(userId))
                .totalPdfs(pdfResultRepository.countByUserId(userId))
                .successPdfs(pdfResultRepository.countByUserIdAndStatus(userId, "DONE"))
                .failedPdfs(pdfResultRepository.countByUserIdAndStatus(userId, "FAILED"))
                .pendingPdfs(pdfResultRepository.countByUserIdAndStatus(userId, "PENDING"))
                .recentPdfs(recentPdfs)
                .build();
    }

    @Override
    public PageResponse<AdminPdfDTO> getAllPdfs(int page, int size, String status) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<PdfResult> pdfPage = (status == null || status.equalsIgnoreCase("ALL"))
                ? pdfResultRepository.findAll(pageable)
                : pdfResultRepository.findByStatus(status.toUpperCase(), pageable);

        // Batch-load users to avoid N+1
        List<Long> userIds = pdfPage.getContent().stream()
                .map(PdfResult::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> userMap = userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<AdminPdfDTO> content = pdfPage.getContent().stream()
                .map(pdf -> toPdfDto(pdf, userMap.get(pdf.getUserId())))
                .collect(Collectors.toList());

        Page<AdminPdfDTO> dtoPage = new PageImpl<>(content, pdfPage.getPageable(), pdfPage.getTotalElements());
        return PageResponse.from(dtoPage);
    }

    @Override
    public List<AdminPdfDTO> getRecentActivity() {
        List<PdfResult> recent = pdfResultRepository.findTop20ByOrderByCreatedAtDesc();

        List<Long> userIds = recent.stream()
                .map(PdfResult::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> userMap = userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return recent.stream()
                .map(pdf -> toPdfDto(pdf, userMap.get(pdf.getUserId())))
                .collect(Collectors.toList());
    }

    // ── Private helpers ───────────────────────────────────────

    private AdminUserSummaryDTO toUserSummary(User user) {
        Long uid = user.getId();
        return AdminUserSummaryDTO.builder()
                .id(uid)
                .name(user.getName())
                .email(user.getEmail())
                .roles(user.getRoles().stream().map(r -> r.name()).collect(Collectors.toSet()))
                .authProvider(user.getAuthProvider().name())
                .accountEnabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .totalSessions(sessionRepository.countByUserID(uid))
                .totalPdfs(pdfResultRepository.countByUserId(uid))
                .successPdfs(pdfResultRepository.countByUserIdAndStatus(uid, "DONE"))
                .failedPdfs(pdfResultRepository.countByUserIdAndStatus(uid, "FAILED"))
                .build();
    }

    private AdminPdfDTO toPdfDto(PdfResult pdf, User user) {
        return AdminPdfDTO.builder()
                .id(pdf.getId())
                .jobId(pdf.getJobId())
                .userId(pdf.getUserId())
                .userEmail(user != null ? user.getEmail() : "unknown")
                .userName(user != null ? user.getName() : "unknown")
                .filename(pdf.getFilename())
                .documentType(pdf.getDocumentType())
                .status(pdf.getStatus())
                .errorMessage(pdf.getErrorMessage())
                .createdAt(pdf.getCreatedAt())
                .updatedAt(pdf.getUpdatedAt())
                .build();
    }
}
