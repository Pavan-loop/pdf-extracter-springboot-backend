package com.madara.security.controller;

import com.madara.security.response.DTO.ApiResponse;
import com.madara.security.response.DTO.SessionDTO;
import com.madara.security.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("session")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createSession(
            @RequestBody SessionDTO sessionDTO
            ) {
        Long id = sessionService.createSession(sessionDTO);
        return ResponseEntity.ok(ApiResponse.success(id, "Created Successfully", HttpStatus.CREATED));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<SessionDTO>>> getAllSessionsOfAUser() {
        List<SessionDTO> sessionDTOS = sessionService.displaySession();
        return ResponseEntity.ok(ApiResponse.success(sessionDTOS, "got all the sessions", HttpStatus.OK));
    }
}
