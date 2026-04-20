package com.madara.security.service;

import com.madara.security.response.DTO.SessionDTO;

import java.util.List;

public interface SessionService {
    Long createSession(SessionDTO sessionDTO);
    List<SessionDTO> displaySession();
}
