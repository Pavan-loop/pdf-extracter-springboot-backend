package com.madara.security.security.authentication;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RegistrationRequest {

    @NotEmpty(message = "Name should not be empty")
    @NotBlank(message = "Name should not be empty")
    private String name;
    @NotEmpty(message = "Username should not be empty")
    @NotBlank(message = "Username should not be empty")
    private String username;
    @NotEmpty(message = "Email should not be empty")
    @NotBlank(message = "Email should not be empty")
    @Email(message = "Email should be valid")
    private String email;
    @NotEmpty(message = "Password should not be empty")
    @NotBlank(message = "Password should not be empty")
    @Size(min = 8, message = "Password size should be more than 8")
    private String password;
    private Long phoneNumber;
}
