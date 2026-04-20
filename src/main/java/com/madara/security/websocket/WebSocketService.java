package com.madara.security.websocket;

import com.madara.security.response.DTO.PdfResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void pushResultToUser(String userEmail, PdfResultDTO result) {
        log.info("Pushing result via WebSocket to user: {}", userEmail);
        messagingTemplate.convertAndSendToUser(
                userEmail,
                "/queue/results",
                result
        );
    }
}
