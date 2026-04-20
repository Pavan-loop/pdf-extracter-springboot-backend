package com.madara.security.service.impl;

import com.madara.security.Exception.type.SessionNotFoundException;
import com.madara.security.Exception.type.UserNotFoundException;
import com.madara.security.model.Session;
import com.madara.security.model.User;
import com.madara.security.repository.SessionRepository;
import com.madara.security.repository.UserRepository;
import com.madara.security.response.DTO.SessionDTO;
import com.madara.security.service.SessionService;
import com.madara.security.utility.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    @Override
    public Long createSession(SessionDTO sessionDTO) {
        long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User Not Found"));
        Session session = Session.builder()
                .sessionName(sessionDTO.getSessionName())
                .documentType(sessionDTO.getDocumentType())
                .description(sessionDTO.getDescription())
                .colorCode(sessionDTO.getColor())
                .user(user)
                .build();

        Session session1 = sessionRepository.save(session);

        return session1.getId();
    }

    @Override
    public List<SessionDTO> displaySession() {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            List<Session> session = sessionRepository.getSessionByUserId(userId);
            if (!session.isEmpty()) {
                return session.stream()
                        .map(s -> SessionDTO.builder()
                                .id(s.getId())
                                .sessionName(s.getSessionName())
                                .color(s.getColorCode())
                                .description(s.getDescription())
                                .documentType(s.getDocumentType())
                                .build())
                        .toList();
            }
            throw new SessionNotFoundException();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }
}
