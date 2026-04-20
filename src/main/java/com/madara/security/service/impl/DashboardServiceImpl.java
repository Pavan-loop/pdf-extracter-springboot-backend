package com.madara.security.service.impl;

import com.madara.security.repository.PdfResultRepository;
import com.madara.security.repository.SessionRepository;
import com.madara.security.response.DTO.DashboardCardsDTO;
import com.madara.security.response.DTO.PDFStatDTO;
import com.madara.security.service.DashboardService;
import com.madara.security.utility.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final SessionRepository sessionRepository;
    private final PdfResultRepository pdfResultRepository;


    @Override
    public DashboardCardsDTO getCardDetails() {

        try {
            long userId = SecurityUtils.getCurrentUserId();
            log.info("user id = {}", userId);
            Long sessionNumber = sessionRepository.countByUserID(userId);
            PDFStatDTO stats = pdfResultRepository.getPdfStats(userId);
            Long total = stats.getTotal();
            Long completed = stats.getSuccess();

            return DashboardCardsDTO.builder()
                    .session(sessionNumber)
                    .records(total)
                    .completed(completed)
                    .build();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return DashboardCardsDTO.builder()
                .session(0L)
                .records(0L)
                .completed(0L)
                .build();
    }
}
