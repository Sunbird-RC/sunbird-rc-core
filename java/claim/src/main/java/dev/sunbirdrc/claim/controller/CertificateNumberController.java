package dev.sunbirdrc.claim.controller;

import dev.sunbirdrc.claim.service.CertificateNumberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CertificateNumberController {

    private CertificateNumberService certificateNumberService;

    @Autowired
    public CertificateNumberController(CertificateNumberService certificateNumberService) {
        this.certificateNumberService = certificateNumberService;
    }

    @GetMapping("/api/v1/generate-certNumber")
    public Long generateNumber() {
        return certificateNumberService.generateAndSaveNumber();
    }
}

