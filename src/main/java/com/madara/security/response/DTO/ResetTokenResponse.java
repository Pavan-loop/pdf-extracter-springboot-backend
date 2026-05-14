package com.madara.security.response.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResetTokenResponse {
    private String resetToken;
}
