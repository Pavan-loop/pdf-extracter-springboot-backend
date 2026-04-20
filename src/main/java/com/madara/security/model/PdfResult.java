package com.madara.security.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pdf_results")
@EntityListeners(AuditingEntityListener.class)
public class PdfResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique ID linking Kafka messages to this record
    @Column(name = "job_id", unique = true, nullable = false)
    private String jobId;

    // The user who uploaded the PDF
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "filename")
    private String filename;

    // INVOICE, BANK_STATEMENT, PURCHASE_ORDER, UNKNOWN
    @Column(name = "document_type")
    private String documentType;

    // PENDING → PROCESSING → DONE / FAILED
    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "PENDING";

    // All extracted fields stored as flexible JSONB
    // Handles nested objects and arrays (line items, transactions etc.)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extracted_data", columnDefinition = "jsonb")
    private Map<String, Object> extractedData;

    // Populated if status = FAILED
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(insertable = false)
    private Instant updatedAt;

    @ManyToOne
    @JoinColumn(name = "session_id")
    @JsonIgnore
    private Session session;
}
