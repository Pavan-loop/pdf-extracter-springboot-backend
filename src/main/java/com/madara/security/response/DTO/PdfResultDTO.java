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
    private String status;
    private String documentType;
    private Map<String, Object> extractedData;
    private String errorMessage;
    private long processedAt;
}
