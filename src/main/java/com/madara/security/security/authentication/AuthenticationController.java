package com.madara.security.security.authentication;

import com.madara.security.model.Role;
import com.madara.security.response.DTO.ApiResponse;
import com.madara.security.response.type.LoginResponse;
import jakarta.validation.Valid;
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
            @RequestBody
            @Valid
            RegistrationRequest request,
            @RequestParam("role") String role
    ) {
        Role upperRole = Role.valueOf(role.toUpperCase());
        authenticationService.registerUser(request, upperRole);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .status(HttpStatus.CREATED)
                .message("User Created")
                .data(null)
                .build();
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> loginUser(
            @RequestBody
            LoginRequest request
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
