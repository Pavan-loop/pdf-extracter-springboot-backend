package com.madara.security.security.authentication;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "Reset token should not be empty")
    private String resetToken;

    @NotBlank(message = "New password should not be empty")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;
}
