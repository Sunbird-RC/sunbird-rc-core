package dev.sunbirdrc.claim.controller;

import dev.sunbirdrc.claim.service.ClaimService;
import dev.sunbirdrc.claim.service.StudentForeignVerificationService;
import dev.sunbirdrc.claim.service.StudentOutsideUpService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class OutsideStudentController {

    @Autowired
    private StudentForeignVerificationService foreignVerificationService;

    @Autowired
    private StudentOutsideUpService studentOutsideUpService;

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

    @GetMapping("/outsideStudent/verify/{id}/{status}")
    public ResponseEntity<String> verifyForeignStudentVerification(@PathVariable String id,
                                                                   @PathVariable String status) {

        claimService.updateOutsideStudentStatus(id, status);

        return new ResponseEntity<>("Outside/Foreign student verification updated", HttpStatus.OK);
    }

    @GetMapping("/outsideStudent/{id}")
    public ResponseEntity<String> getOutsideStudentVerificationDetail(@PathVariable String id) {
        String template = studentOutsideUpService.generatePendingMailContent(id);

        if (!StringUtils.isEmpty(template)) {
            return new ResponseEntity<>(template, HttpStatus.OK);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
