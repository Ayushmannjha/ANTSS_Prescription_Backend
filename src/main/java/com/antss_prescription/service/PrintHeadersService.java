package com.antss_prescription.service;

import com.antss_prescription.entity.prescription.PrintHeaders;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PrintHeadersService {

    PrintHeaders uploadHeader(long entityId, String entityType, MultipartFile image);

    List<PrintHeaders> getHeaders(Long entityId, String entityType);
}
