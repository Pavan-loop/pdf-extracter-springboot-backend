package com.madara.security.security.authentication;

import com.madara.security.Exception.type.InvalidOtpException;
import com.madara.security.Exception.type.UnauthorizedException;
import com.madara.security.Exception.type.UserAlreadyExistException;
import com.madara.security.Exception.type.UserNotFoundException;
import com.madara.security.model.AuthProvider;
import com.madara.security.model.Otp;
import com.madara.security.model.OtpType;
import com.madara.security.model.PasswordResetToken;
import com.madara.security.model.Role;
import com.madara.security.model.User;
import com.madara.security.repository.OtpRepository;
import com.madara.security.repository.PasswordResetTokenRepository;
import com.madara.security.repository.UserRepository;
import com.madara.security.response.DTO.ResetTokenResponse;
import com.madara.security.security.jwt.JwtService;
import com.madara.security.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final EmailService emailService;

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int RESET_TOKEN_EXPIRY_MINUTES = 15;

    @Transactional
    public void registerUser(RegistrationRequest request, Role role) {
        Optional<User> existing = userRepository.findByEmail(request.getEmail());
        if (existing.isPresent()) {
            throw new UserAlreadyExistException("This user already exists");
        }

        var user = User.builder()
                .name(request.getName())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .roles(new HashSet<>(Set.of(role)))
                .isAccountEnabled(false)
                .isAccountNonLocked(true)
                .build();

        userRepository.save(user);
        sendOtp(request.getEmail(), OtpType.REGISTRATION);
    }

    @Transactional
    public void verifyOtp(OtpVerificationRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new InvalidOtpException("OTP verification is only for manually registered accounts");
        }

        Otp otp = otpRepository
                .findTopByEmailAndOtpTypeAndUsedFalseOrderByExpiresAtDesc(request.getEmail(), OtpType.REGISTRATION)
                .orElseThrow(() -> new InvalidOtpException("No active OTP found. Please request a new one"));

        validateOtp(otp, request.getOtpCode());

        otp.setUsed(true);
        otpRepository.save(otp);

        user.setAccountEnabled(true);
        userRepository.save(user);

        if (user.getRoles().contains(Role.ADMIN)) {
            userRepository.findSeededAdmin()
                    .filter(seeded -> !seeded.getId().equals(user.getId()))
                    .ifPresent(seeded -> {
                        otpRepository.deleteByEmailAndOtpType(seeded.getEmail(), OtpType.REGISTRATION);
                        userRepository.delete(seeded);
                    });
        }
    }

    @Transactional
    public void resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.isEnabled()) {
            throw new InvalidOtpException("Account is already verified");
        }

        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new InvalidOtpException("OTP verification is only for manually registered accounts");
        }

        otpRepository.deleteByEmailAndOtpType(email, OtpType.REGISTRATION);
        sendOtp(email, OtpType.REGISTRATION);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("No account found with this email"));

        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new InvalidOtpException("This account uses Google login. Password reset is not available");
        }

        otpRepository.deleteByEmailAndOtpType(request.getEmail(), OtpType.PASSWORD_RESET);
        passwordResetTokenRepository.deleteByEmail(request.getEmail());
        sendOtp(request.getEmail(), OtpType.PASSWORD_RESET);
    }

    @Transactional
    public ResetTokenResponse verifyResetOtp(OtpVerificationRequest request) {
        userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("No account found with this email"));

        Otp otp = otpRepository
                .findTopByEmailAndOtpTypeAndUsedFalseOrderByExpiresAtDesc(request.getEmail(), OtpType.PASSWORD_RESET)
                .orElseThrow(() -> new InvalidOtpException("No active reset code found. Please request a new one"));

        validateOtp(otp, request.getOtpCode());

        otp.setUsed(true);
        otpRepository.save(otp);

        String token = UUID.randomUUID().toString();
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .token(token)
                .email(request.getEmail())
                .expiresAt(Instant.now().plusSeconds(RESET_TOKEN_EXPIRY_MINUTES * 60L))
                .build());

        return new ResetTokenResponse(token);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenAndUsedFalse(request.getResetToken())
                .orElseThrow(() -> new InvalidOtpException("Invalid or expired reset token. Please start over"));

        if (Instant.now().isAfter(resetToken.getExpiresAt())) {
            throw new InvalidOtpException("Reset token has expired. Please start over");
        }

        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new UserNotFoundException("No account found"));

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public String loginAndGenerateJwtToken(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new UnauthorizedException("Invalid Email or Password");
        } catch (DisabledException e) {
            throw new UnauthorizedException("Account is not verified. Please check your email for the OTP");
        } catch (AuthenticationException e) {
            throw new UnauthorizedException("This account is not authenticated");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        return jwtService.generateToken(userDetails);
    }

    private void validateOtp(Otp otp, String submittedCode) {
        if (Instant.now().isAfter(otp.getExpiresAt())) {
            throw new InvalidOtpException("OTP has expired. Please request a new one");
        }
        if (!otp.getOtpCode().equals(submittedCode)) {
            throw new InvalidOtpException("Invalid OTP code");
        }
    }

    private void sendOtp(String email, OtpType type) {
        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        Otp otp = Otp.builder()
                .email(email)
                .otpCode(code)
                .otpType(type)
                .expiresAt(Instant.now().plusSeconds(OTP_EXPIRY_MINUTES * 60L))
                .build();
        otpRepository.save(otp);

        if (type == OtpType.PASSWORD_RESET) {
            emailService.sendPasswordResetEmail(email, code);
        } else {
            emailService.sendOtpEmail(email, code);
        }
    }
}
