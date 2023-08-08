package dev.sunbirdrc.claim.controller;

import dev.sunbirdrc.claim.entity.Learner;
import dev.sunbirdrc.claim.service.CredentialsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/credentials")
public class CredentialsController {
    @Autowired
    private CredentialsService credentialService;

    public CredentialsController(CredentialsService credentialService) {
        this.credentialService = credentialService;
    }


    @PostMapping("/save")
    public ResponseEntity<String> saveLearnerWithCredentials(@RequestBody Learner learner) {
        credentialService.saveLearnerWithCredentials(learner);
        return ResponseEntity.ok("Learner with credentials saved successfully");
    }


    @GetMapping("/get")
    public ResponseEntity<List<Learner>> getAllLearnerWithCredentials() {
        List<Learner> learner = credentialService.getAllLearnerWithCredentials();
        return ResponseEntity.ok(learner);
    }

    @GetMapping("/get/{learnerId}")
    public ResponseEntity<Learner> getLearnerWithCredentialsById(@PathVariable Long learnerId) {
        Learner learner = credentialService.getLearnerWithCredentialsById(learnerId);
        if (learner != null) {
            return ResponseEntity.ok(learner);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    @GetMapping("/getByName/{learnerName}")
    public ResponseEntity<List<Learner>> getLearnerWithCredentialsByName(@PathVariable String learnerName) {
        List<Learner> learners = credentialService.getLearnerWithCredentialsByName(learnerName);
        if (!learners.isEmpty()) {
            return ResponseEntity.ok(learners);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    @GetMapping("/getByRollNumber/{rollNumber}")
    public ResponseEntity<Learner> getLearnerWithCredentialsByRollNumber(@PathVariable String rollNumber) {
        Learner learner = credentialService.getLearnerWithCredentialsByRollNumber(rollNumber);
        if (learner != null) {
            return ResponseEntity.ok(learner);
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @PutMapping("/update/{learnerId}")
    public ResponseEntity<Learner> updateLearnerWithCredentials(@PathVariable Long learnerId, @RequestBody Learner learner) {
        Learner existinglearner = credentialService.getLearnerWithCredentialsById(learnerId);
        if (existinglearner != null) {
            learner.setId(existinglearner.getId());
            Learner updatedLearner = credentialService.updateLearnerWithCredentials(learner);
            return ResponseEntity.ok(updatedLearner);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/delete/{learnerId}")
    public ResponseEntity<String> deleteLearnerWithCredentials(@PathVariable Long learnerId) {
        Learner existinglearner = credentialService.getLearnerWithCredentialsById(learnerId);
        if (existinglearner != null) {
            credentialService.deleteLearnerWithCredentials(learnerId);
            return ResponseEntity.ok("Learner with credentials deleted successfully");
        } else {
            return ResponseEntity.notFound().build();
        }
    }


}

