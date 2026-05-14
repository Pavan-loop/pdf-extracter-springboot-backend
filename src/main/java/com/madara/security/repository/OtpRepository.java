package com.madara.security.repository;

import com.madara.security.model.Otp;
import com.madara.security.model.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findTopByEmailAndOtpTypeAndUsedFalseOrderByExpiresAtDesc(String email, OtpType otpType);

    void deleteByEmailAndOtpType(String email, OtpType otpType);
}
