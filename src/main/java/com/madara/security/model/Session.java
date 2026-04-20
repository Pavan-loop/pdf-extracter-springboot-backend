package com.madara.security.model;


import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "session")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "Name")
    private String sessionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "DocumentType")
    private DocumentType documentType;

    @Column(name = "Description")
    private String description;

    @Column(name = "ColorCode")
    private String colorCode;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PdfResult> pdfResults;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
