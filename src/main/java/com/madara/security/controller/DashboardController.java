package com.madara.security.controller;

import com.madara.security.response.DTO.ApiResponse;
import com.madara.security.response.DTO.DashboardCardsDTO;
import com.madara.security.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardCardsDTO>> getDashboardCardsData() {
        DashboardCardsDTO result = dashboardService.getCardDetails();
        return ResponseEntity.ok(ApiResponse.success(result, "Fetched Successfully", HttpStatus.OK));
    }
}
