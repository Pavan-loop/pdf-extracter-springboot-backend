package com.madara.security.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madara.security.model.PdfResult;
import com.madara.security.model.Session;
import com.madara.security.model.User;
import com.madara.security.repository.PdfResultRepository;
import com.madara.security.repository.SessionRepository;
import com.madara.security.repository.UserRepository;
import com.madara.security.response.DTO.PDFFilePathAndUserIDDTO;
import com.madara.security.response.DTO.PdfResultDTO;
import com.madara.security.service.PDFService;
import com.madara.security.utility.SecurityUtils;
import com.madara.security.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PDFServiceImpl implements PDFService {

    @Value("${application.file.upload-dir}")
    private String uploadDir;

    private final KafkaTemplate<String, PDFFilePathAndUserIDDTO> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final PdfResultRepository pdfResultRepository;
    private final UserRepository userRepository;
    private final WebSocketService webSocketService;
    private final SessionRepository sessionRepository;

    @Override
    public void Store(MultipartFile pdf) throws IOException {
        if (pdf.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        String contentType = pdf.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException("Only PDF files are accepted: " + pdf.getOriginalFilename());
        }

        String originalFilename = pdf.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Invalid filename");
        }

        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);
        Path filePath = uploadPath.resolve(originalFilename);

        long userId = SecurityUtils.getCurrentUserId();
        String jobId = String.valueOf(System.currentTimeMillis());

        pdf.transferTo(filePath.toFile());

        PdfResult pendingResult = PdfResult.builder()
                .jobId(jobId)
                .userId(userId)
                .filename(originalFilename)
                .status("PENDING")
                .documentType("PURCHASE_ORDER")
                .build();
        pdfResultRepository.save(pendingResult);

        kafkaTemplate.send(
                "pdf-topic",
                jobId,
                PDFFilePathAndUserIDDTO.builder()
                        .jobID(Long.parseLong(jobId))
                        .userId(userId)
                        .pdfFilePath(filePath.toString())
                        .documentType("PURCHASE_ORDER")
                        .build()
        );

        log.info("PDF queued. jobId={}, userId={}, file={}", jobId, userId, originalFilename);
    }

    @Override
    @KafkaListener(topics = "pdf-response", groupId = "spring-pdf-group")
    public void receivesData(ConsumerRecord<String, String> record) {
        String jobId = record.key();
        log.info("Received result from Python worker for jobId={}", jobId);

        try {
            PdfResultDTO result = objectMapper.readValue(record.value(), PdfResultDTO.class);

            PdfResult pdfResult = pdfResultRepository.findByJobId(jobId)
                    .orElseGet(() -> {
                        log.warn("No DB record found for jobId={}, creating new one", jobId);
                        return PdfResult.builder()
                                .jobId(jobId)
                                .userId(0L)
                                .build();
                    });

            pdfResult.setStatus(result.getStatus());
            pdfResult.setDocumentType(result.getDocumentType());
            pdfResult.setExtractedData(result.getExtractedData());
            pdfResult.setErrorMessage(result.getErrorMessage());
            pdfResultRepository.save(pdfResult);

            log.info("DB updated for jobId={}, status={}, docType={}",
                    jobId, result.getStatus(), result.getDocumentType());

            // FIX: convertAndSendToUser routes by principal name = email, NOT numeric userId.
            // Look up the user's email from DB so the message reaches the right STOMP session.
            String userEmail = userRepository.findById(pdfResult.getUserId())
                    .map(User::getEmail)
                    .orElse(String.valueOf(pdfResult.getUserId())); // fallback — will fail silently

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
        List<PdfResult> pdfResults = pdfResultRepository.getPdfResultBySessionId(sessionId);

        if (!pdfResults.isEmpty()) {
            return pdfResults;
        }
        return null;
    }

    @Override
    public void StoreBySession(MultipartFile pdf, Long sessionId) throws IOException {
        if (pdf.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        String contentType = pdf.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException("Only PDF files are accepted: " + pdf.getOriginalFilename());
        }

        String originalFilename = pdf.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Invalid filename");
        }

        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);
        Path filePath = uploadPath.resolve(originalFilename);

        long userId = SecurityUtils.getCurrentUserId();
        String jobId = String.valueOf(System.currentTimeMillis());

        pdf.transferTo(filePath.toFile());

        Session session = sessionRepository.getSessionById(sessionId);

        PdfResult pendingResult = PdfResult.builder()
                .jobId(jobId)
                .userId(userId)
                .filename(originalFilename)
                .status("PENDING")
                .documentType("PURCHASE_ORDER")
                .session(session)
                .build();
        pdfResultRepository.save(pendingResult);

        kafkaTemplate.send(
                "pdf-topic",
                jobId,
                PDFFilePathAndUserIDDTO.builder()
                        .jobID(Long.parseLong(jobId))
                        .userId(userId)
                        .pdfFilePath(filePath.toString())
                        .documentType("PURCHASE_ORDER")
                        .build()
        );

        log.info("PDF queued. jobId={}, userId={}, file={}", jobId, userId, originalFilename);
    }
}
