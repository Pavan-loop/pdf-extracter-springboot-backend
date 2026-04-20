package com.madara.security.response.DTO;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfResultDTO {

    private String jobId;

    // DONE or FAILED
    private String status;

    // INVOICE, BANK_STATEMENT, PURCHASE_ORDER, UNKNOWN
    private String documentType;

    // All extracted fields — flexible, matches whatever Python returns
    // e.g. { "vendor": "Acme", "line_items": [...] }
    private Map<String, Object> extractedData;

    // Populated if status = FAILED
    private String errorMessage;

    private long processedAt;
}
