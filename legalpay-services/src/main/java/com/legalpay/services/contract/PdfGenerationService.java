package com.legalpay.services.contract;

import com.legalpay.domain.entity.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * DUMMY PDF Generation Service
 * In production: Use iText to generate actual PDFs and upload to Cloudflare R2/AWS S3
 */
@Service
public class PdfGenerationService {

    private static final Logger log = LoggerFactory.getLogger(PdfGenerationService.class);

    public String generateContractPdf(Contract contract) {
        log.info("DUMMY: Generating PDF for contract {}", contract.getId());
        
        // DUMMY: Return a fake URL
        // In production:
        // 1. Generate PDF using iText
        // 2. Upload to R2/S3
        // 3. Return public URL
        
        return "https://storage.legalpay.in/contracts/" + contract.getId() + ".pdf";
    }
}
