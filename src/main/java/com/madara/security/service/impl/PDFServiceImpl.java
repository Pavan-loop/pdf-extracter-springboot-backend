package com.madara.security.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madara.security.Exception.type.PdfNotFoundException;
import com.madara.security.Exception.type.QuotaExceededException;
import com.madara.security.Exception.type.UserNotFoundException;
import com.madara.security.model.PdfResult;
import com.madara.security.model.Session;
import com.madara.security.model.User;
import com.madara.security.repository.PdfResultRepository;
import com.madara.security.repository.SessionRepository;
import com.madara.security.repository.UserRepository;
import com.madara.security.response.DTO.PDFFilePathAndUserIDDTO;
import com.madara.security.response.DTO.PdfResultDTO;
import com.madara.security.service.FileStorageService;
import com.madara.security.service.PDFService;
import com.madara.security.utility.SecurityUtils;
import com.madara.security.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PDFServiceImpl implements PDFService {

    @Value("${application.kafka.topic.pdf-request}")
    private String pdfRequestTopic;

    private final KafkaTemplate<String, PDFFilePathAndUserIDDTO> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final PdfResultRepository pdfResultRepository;
    private final UserRepository userRepository;
    private final WebSocketService webSocketService;
    private final SessionRepository sessionRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public void Store(MultipartFile pdf) throws IOException {
        validateFile(pdf);
        long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        int pageCount = countPages(pdf);
        enforceQuota(user, pageCount);

        String jobId = UUID.randomUUID().toString();
        String storedPath = fileStorageService.store(jobId, pdf.getOriginalFilename(), pdf);

        PdfResult pending = PdfResult.builder()
                .jobId(jobId)
                .userId(userId)
                .filename(pdf.getOriginalFilename())
                .status("PENDING")
                .documentType("PURCHASE_ORDER")
                .filePath(storedPath)
                .build();
        pdfResultRepository.save(pending);

        kafkaTemplate.send(pdfRequestTopic, jobId,
                PDFFilePathAndUserIDDTO.builder()
                        .jobId(jobId)
                        .userId(userId)
                        .pdfFilePath(storedPath)
                        .documentType("PURCHASE_ORDER")
                        .isSession(false)
                        .build());

        log.info("PDF queued jobId={} userId={} file={} pages={}", jobId, userId, pdf.getOriginalFilename(), pageCount);
    }

    @Override
    @Transactional
    public void StoreBySession(MultipartFile pdf, Long sessionId) throws IOException {
        validateFile(pdf);
        long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        int pageCount = countPages(pdf);
        enforceQuota(user, pageCount);

        Session session = sessionRepository.getSessionById(sessionId);

        String jobId = UUID.randomUUID().toString();
        String storedPath = fileStorageService.store(jobId, pdf.getOriginalFilename(), pdf);

        PdfResult pending = PdfResult.builder()
                .jobId(jobId)
                .userId(userId)
                .filename(pdf.getOriginalFilename())
                .status("PENDING")
                .documentType(session.getDocumentType().toString())
                .filePath(storedPath)
                .session(session)
                .build();
        pdfResultRepository.save(pending);

        kafkaTemplate.send(pdfRequestTopic, jobId,
                PDFFilePathAndUserIDDTO.builder()
                        .jobId(jobId)
                        .userId(userId)
                        .pdfFilePath(storedPath)
                        .documentType(session.getDocumentType().toString())
                        .isSession(true)
                        .build());

        log.info("PDF queued jobId={} userId={} sessionId={} file={}", jobId, userId, sessionId, pdf.getOriginalFilename());
    }

    @Override
    @KafkaListener(topics = "${application.kafka.topic.pdf-response}", groupId = "spring-pdf-group")
    public void receivesData(ConsumerRecord<String, String> record) {
        String jobId = record.key();
        log.info("Received result from Python worker for jobId={}", jobId);

        try {
            PdfResultDTO result = objectMapper.readValue(record.value(), PdfResultDTO.class);

            PdfResult pdfResult = pdfResultRepository.findByJobId(jobId)
                    .orElseGet(() -> {
                        log.warn("No DB record found for jobId={}, creating new one", jobId);
                        return PdfResult.builder().jobId(jobId).userId(0L).build();
                    });

            pdfResult.setStatus(result.getStatus());
            pdfResult.setDocumentType(result.getDocumentType());
            pdfResult.setExtractedData(result.getExtractedData());
            pdfResult.setErrorMessage(result.getErrorMessage());
            pdfResultRepository.save(pdfResult);

            log.info("DB updated jobId={} status={}", jobId, result.getStatus());

            String userEmail = userRepository.findById(pdfResult.getUserId())
                    .map(User::getEmail)
                    .orElse(String.valueOf(pdfResult.getUserId()));

            result.setJobId(jobId);
            webSocketService.pushResultToUser(userEmail, result);

        } catch (Exception e) {
            log.error("Failed to process Kafka result for jobId={}: {}", jobId, e.getMessage(), e);
            pdfResultRepository.findByJobId(jobId).ifPresent(r -> {
                r.setStatus("FAILED");
                r.setErrorMessage("Processing error: " + e.getMessage());
                pdfResultRepository.save(r);
            });
        }
    }

    @Override
    public List<PdfResult> getPdfsRelatedToSession(Long sessionId) {
        List<PdfResult> results = pdfResultRepository.getPdfResultBySessionId(sessionId);
        return results.isEmpty() ? List.of() : results;
    }

    @Override
    @Transactional
    public void deletePdf(Long id) {
        PdfResult pdf = pdfResultRepository.findById(id)
                .orElseThrow(PdfNotFoundException::new);

        if (pdf.getFilePath() != null) {
            fileStorageService.delete(pdf.getFilePath());
        }

        pdfResultRepository.deleteById(id);
        log.info("PDF record and file deleted id={}", id);
    }

    @Override
    @Transactional
    public void bulkDeleteId(List<Long> ids) {
        List<PdfResult> pdfs = pdfResultRepository.findAllById(ids);
        pdfs.forEach(pdf -> {
            if (pdf.getFilePath() != null) {
                fileStorageService.delete(pdf.getFilePath());
            }
        });
        pdfResultRepository.deleteAllByIdInBatch(ids);
        log.info("Bulk deleted {} PDF records", ids.size());
    }

    // ── Private helpers ───────────────────────────────────────

    private void validateFile(MultipartFile pdf) {
        if (pdf.isEmpty()) throw new IllegalArgumentException("Uploaded file is empty");
        String ct = pdf.getContentType();
        if (ct == null || !ct.equals("application/pdf"))
            throw new IllegalArgumentException("Only PDF files are accepted: " + pdf.getOriginalFilename());
        String name = pdf.getOriginalFilename();
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Invalid filename");
    }

    private int countPages(MultipartFile pdf) {
        try (PDDocument doc = Loader.loadPDF(pdf.getBytes())) {
            return doc.getNumberOfPages();
        } catch (Exception e) {
            log.warn("Could not count pages for {}: {}", pdf.getOriginalFilename(), e.getMessage());
            return 1;
        }
    }

    private void enforceQuota(User user, int incomingPages) {
        if (user.getPlan().isUnlimited()) return;

        Instant startOfMonth = YearMonth.now(ZoneOffset.UTC).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        if (user.getQuotaResetAt() == null || user.getQuotaResetAt().isBefore(startOfMonth)) {
            user.setPagesUploadedThisMonth(0);
            user.setQuotaResetAt(startOfMonth);
        }

        int used = user.getPagesUploadedThisMonth();
        int limit = user.getPlan().getMonthlyPageLimit();
        int remaining = limit - used;

        if (incomingPages > remaining) {
            throw new QuotaExceededException(String.format(
                    "Monthly page limit reached. Your %s plan allows %d pages/month. " +
                    "Used: %d, Remaining: %d, This PDF: %d pages. Please upgrade your plan.",
                    user.getPlan().name(), limit, used, remaining, incomingPages));
        }

        user.setPagesUploadedThisMonth(used + incomingPages);
        userRepository.save(user);
    }
}
