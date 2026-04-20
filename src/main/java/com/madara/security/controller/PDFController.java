package com.madara.security.controller;

import com.madara.security.Exception.type.UserNotFoundException;
import com.madara.security.model.PdfResult;
import com.madara.security.repository.PdfResultRepository;
import com.madara.security.response.DTO.ApiResponse;
import com.madara.security.service.PDFService;
import com.madara.security.utility.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("pdf")
@RequiredArgsConstructor
public class PDFController {

    private final PDFService pdfService;
    private final PdfResultRepository pdfResultRepository;

    /**
     * Upload one or multiple PDFs at once.
     * Frontend sends: multipart/form-data with field name "files" (can repeat multiple times)
     * Example curl:
     *   curl -F "files=@po1.pdf" -F "files=@po2.pdf" http://localhost:8081/pdf/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Void>> uploadPDFs(
            @RequestParam("files") List<MultipartFile> files
    ) throws IOException {
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.success(null, "No files provided", HttpStatus.BAD_REQUEST)
            );
        }

        for (MultipartFile file : files) {
            pdfService.Store(file);
        }

        String message = files.size() == 1
                ? "1 PDF uploaded and queued for processing"
                : files.size() + " PDFs uploaded and queued for processing";

        return new ResponseEntity<>(
                ApiResponse.success(null, message, HttpStatus.OK),
                HttpStatus.OK
        );
    }

    // Poll for a specific job result (fallback if WebSocket disconnects)
    @GetMapping("/result/{jobId}")
    public ResponseEntity<ApiResponse<PdfResult>> getResult(
            @PathVariable String jobId
    ) {
        PdfResult result = pdfResultRepository.findByJobId(jobId)
                .orElseThrow(() -> new UserNotFoundException("Job not found: " + jobId));
        return ResponseEntity.ok(ApiResponse.success(result, "Result fetched", HttpStatus.OK));
    }

    // Get all PDFs uploaded by the currently logged-in user
    @GetMapping("/my-results")
    public ResponseEntity<ApiResponse<List<PdfResult>>> getMyResults() {
        long userId = SecurityUtils.getCurrentUserId();
        List<PdfResult> results = pdfResultRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(ApiResponse.success(results, "Results fetched", HttpStatus.OK));
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ApiResponse<List<PdfResult>>> getPdfsBySessionId (
            @PathVariable Long sessionId
    ) {
        List<PdfResult> results = pdfService.getPdfsRelatedToSession(sessionId);
        return ResponseEntity.ok(ApiResponse.success(results, "Results fetched", HttpStatus.OK));
    }

    @PostMapping("/upload/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> uploadPDFsOnSession(
            @RequestParam("files") List<MultipartFile> files,
            @PathVariable Long sessionId
    ) throws IOException {
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.success(null, "No files provided", HttpStatus.BAD_REQUEST)
            );
        }

        for (MultipartFile file : files) {
            pdfService.StoreBySession(file, sessionId);
        }

        String message = files.size() == 1
                ? "1 PDF uploaded and queued for processing"
                : files.size() + " PDFs uploaded and queued for processing";

        return new ResponseEntity<>(
                ApiResponse.success(null, message, HttpStatus.OK),
                HttpStatus.OK
        );
    }
}