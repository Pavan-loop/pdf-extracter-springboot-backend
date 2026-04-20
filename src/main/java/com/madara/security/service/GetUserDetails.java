package com.madara.security.service;

import com.madara.security.utility.SecurityUtils;
import org.springframework.stereotype.Service;

@Service
public class GetUserDetails {
    public long getUserId() {
        return SecurityUtils.getCurrentUserId();
    }
}
