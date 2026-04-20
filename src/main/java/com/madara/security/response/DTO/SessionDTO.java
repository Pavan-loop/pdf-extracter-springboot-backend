package com.madara.security.response.DTO;

import com.madara.security.model.DocumentType;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDTO {
    private Long id;
    private String sessionName;
    private DocumentType documentType;
    private String description;
    private String color;
}
