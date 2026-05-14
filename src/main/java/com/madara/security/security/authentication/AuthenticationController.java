package com.madara.security.security.authentication;

import com.madara.security.model.Role;
import com.madara.security.response.DTO.ApiResponse;
import com.madara.security.response.DTO.ResetTokenResponse;
import com.madara.security.response.type.LoginResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> registerUser(
            @RequestBody @Valid RegistrationRequest request
    ) {
        authenticationService.registerUser(request, Role.USER);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .status(HttpStatus.CREATED)
                .message("Registration successful. Please check your email for the OTP to verify your account")
                .data(null)
                .build();
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(
            @RequestBody @Valid OtpVerificationRequest request
    ) {
        authenticationService.verifyOtp(request);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .status(HttpStatus.OK)
                .message("Email verified successfully. You can now log in")
                .data(null)
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(
            @RequestParam @NotBlank @Email String email
    ) {
        authenticationService.resendOtp(email);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .status(HttpStatus.OK)
                .message("OTP resent. Please check your email")
                .data(null)
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @RequestBody @Valid ForgotPasswordRequest request
    ) {
        authenticationService.forgotPassword(request);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .status(HttpStatus.OK)
                .message("Password reset code sent. Please check your email")
                .data(null)
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<ApiResponse<ResetTokenResponse>> verifyResetOtp(
            @RequestBody @Valid OtpVerificationRequest request
    ) {
        ResetTokenResponse data = authenticationService.verifyResetOtp(request);
        ApiResponse<ResetTokenResponse> response = ApiResponse.<ResetTokenResponse>builder()
                .success(true)
                .status(HttpStatus.OK)
                .message("OTP verified. You can now reset your password")
                .data(data)
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestBody @Valid ResetPasswordRequest request
    ) {
        authenticationService.resetPassword(request);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .status(HttpStatus.OK)
                .message("Password reset successfully. You can now log in with your new password")
                .data(null)
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> loginUser(
            @RequestBody LoginRequest request
    ) {
        String token = authenticationService.loginAndGenerateJwtToken(request);
        ApiResponse<LoginResponse> response = ApiResponse.<LoginResponse>builder()
                .success(true)
                .status(HttpStatus.ACCEPTED)
                .message("Login successful")
                .data(new LoginResponse(token))
                .build();
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }
}
