package dev.sunbirdrc.claim.controller;

import dev.sunbirdrc.claim.service.ClaimService;
import dev.sunbirdrc.claim.service.StudentForeignVerificationService;
import dev.sunbirdrc.claim.status.Status;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/outside")
public class OutsideStudentController {

    @Autowired
    private StudentForeignVerificationService foreignVerificationService;

    @Autowired
    private ClaimService claimService;

    @GetMapping("/foreignStudent/{id}")
    public ResponseEntity<String> getForeignStudentVerificationDetail(@PathVariable String id) {
        String template = foreignVerificationService.generatePendingMailContent(id);

        if (!StringUtils.isEmpty(template)) {
            return new ResponseEntity<>(template, HttpStatus.OK);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/foreignStudent/verify/{id}/{status}")
    public ResponseEntity<String> verifyForeignStudentVerification(@PathVariable String id,
                                                                   @PathVariable Status status) {

        claimService.updateForeignStudentStatus(id, status);

        return new ResponseEntity<>("Foreign student verification updated", HttpStatus.OK);
    }
}
