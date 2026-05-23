package com.madara.security.service.impl;

import com.madara.security.model.User;
import com.madara.security.repository.PdfResultRepository;
import com.madara.security.repository.SessionRepository;
import com.madara.security.repository.UserRepository;
import com.madara.security.response.DTO.DashboardCardsDTO;
import com.madara.security.response.DTO.PDFStatDTO;
import com.madara.security.service.DashboardService;
import com.madara.security.utility.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final SessionRepository sessionRepository;
    private final PdfResultRepository pdfResultRepository;
    private final UserRepository userRepository;

    @Override
    public DashboardCardsDTO getCardDetails() {
        try {
            long userId = SecurityUtils.getCurrentUserId();

            User user = userRepository.findById(userId).orElseThrow();
            Long sessionNumber = sessionRepository.countByUserID(userId);
            PDFStatDTO stats = pdfResultRepository.getPdfStats(userId);

            int limit = user.getPlan().getMonthlyPageLimit();
            int used  = user.getPagesUploadedThisMonth();
            int remaining = user.getPlan().isUnlimited() ? Integer.MAX_VALUE : Math.max(0, limit - used);

            return DashboardCardsDTO.builder()
                    .session(sessionNumber)
                    .records(stats.getTotal())
                    .completed(stats.getSuccess())
                    .plan(user.getPlan().name())
                    .pagesUploadedThisMonth(used)
                    .monthlyPageLimit(limit)
                    .pagesRemaining(remaining)
                    .unlimited(user.getPlan().isUnlimited())
                    .subscriptionExpiresAt(
                            user.getSubscriptionExpiresAt() != null
                                    ? user.getSubscriptionExpiresAt().toString()
                                    : null
                    )
                    .build();

        } catch (Exception e) {
            log.error("Failed to fetch dashboard: {}", e.getMessage());
            return DashboardCardsDTO.builder()
                    .session(0L).records(0L).completed(0L)
                    .plan("FREE").pagesUploadedThisMonth(0)
                    .monthlyPageLimit(10).pagesRemaining(10)
                    .unlimited(false).subscriptionExpiresAt(null)
                    .build();
        }
    }
}
