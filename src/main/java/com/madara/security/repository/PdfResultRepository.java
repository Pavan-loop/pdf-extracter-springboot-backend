package com.madara.security.repository;

import com.madara.security.model.PdfResult;
import com.madara.security.response.DTO.PDFStatDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PdfResultRepository extends JpaRepository<PdfResult, Long> {

    Optional<PdfResult> findByJobId(String jobId);

    // All PDFs uploaded by a specific user
    List<PdfResult> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query(value = """
    SELECT 
        COUNT(*) AS total,
        SUM(CASE WHEN status = 'DONE' THEN 1 ELSE 0 END) AS success
    FROM pdf_results
    WHERE user_id = :userId
""", nativeQuery = true)
    PDFStatDTO getPdfStats(@Param("userId") Long userId);

    List<PdfResult> getPdfResultBySessionId(Long sessionId);
}
