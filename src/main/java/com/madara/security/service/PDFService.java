package com.madara.security.service;

import com.madara.security.model.PdfResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface PDFService {
    void Store(MultipartFile pdf) throws IOException;
    void receivesData(ConsumerRecord<String, String> record);
    List<PdfResult> getPdfsRelatedToSession(Long sessionId);
    void StoreBySession(MultipartFile pdf, Long sessionId) throws IOException;
    void deletePdf(Long id);
    void bulkDeleteId(List<Long> ids);
}
