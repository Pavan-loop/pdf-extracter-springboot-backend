package com.madara.security.repository;

import com.madara.security.model.Otp;
import com.madara.security.model.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findTopByEmailAndOtpTypeAndUsedFalseOrderByExpiresAtDesc(String email, OtpType otpType);

    void deleteByEmailAndOtpType(String email, OtpType otpType);

    @Modifying
    @Query("DELETE FROM Otp o WHERE o.expiresAt < :now OR o.used = true")
    int deleteExpiredOrUsed(@Param("now") Instant now);
}
