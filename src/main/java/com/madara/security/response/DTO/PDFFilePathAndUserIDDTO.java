package com.madara.security.response.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PDFFilePathAndUserIDDTO {
    private String jobId;
    private String pdfFilePath;
    private long userId;
    private String documentType;
}
