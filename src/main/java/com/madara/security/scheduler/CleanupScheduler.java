package com.madara.security.scheduler;

import com.madara.security.model.Plan;
import com.madara.security.model.User;
import com.madara.security.repository.OtpRepository;
import com.madara.security.repository.PasswordResetTokenRepository;
import com.madara.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CleanupScheduler {

    private final OtpRepository otpRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;

    // Every hour — delete expired/used OTPs
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredOtps() {
        int deleted = otpRepository.deleteExpiredOrUsed(Instant.now());
        if (deleted > 0) log.info("Cleanup: removed {} expired/used OTPs", deleted);
    }

    // Every hour — delete expired/used reset tokens
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredResetTokens() {
        int deleted = passwordResetTokenRepository.deleteExpiredOrUsed(Instant.now());
        if (deleted > 0) log.info("Cleanup: removed {} expired/used reset tokens", deleted);
    }

    // Daily at midnight — downgrade expired subscribers back to FREE
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void downgradeExpiredSubscriptions() {
        List<User> expired = userRepository.findExpiredSubscribers(Instant.now());
        if (expired.isEmpty()) return;

        for (User user : expired) {
            user.setPlan(Plan.FREE);
            user.setSubscriptionExpiresAt(null);
            user.setPagesUploadedThisMonth(0);
            user.setQuotaResetAt(null);
        }
        userRepository.saveAll(expired);
        log.info("Subscriptions: downgraded {} expired subscribers to FREE", expired.size());
    }
}
