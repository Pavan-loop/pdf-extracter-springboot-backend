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

    /**
     * Push the PDF extraction result to the user who uploaded it.
     *
     * IMPORTANT: convertAndSendToUser(username, ...) routes by the STOMP principal name,
     * which is UserDetails.getUsername() = the user's EMAIL address.
     * Do NOT pass the numeric userId here — it won't match the authenticated principal.
     *
     * Frontend subscribes to: /user/queue/results
     * Spring resolves that to: /user/{email}/queue/results  (internally)
     */
    public void pushResultToUser(String userEmail, PdfResultDTO result) {
        log.info("Pushing result via WebSocket to user: {}", userEmail);
        messagingTemplate.convertAndSendToUser(
                userEmail,        // must be the email (= principal name), NOT numeric userId
                "/queue/results",
                result
        );
    }
}
