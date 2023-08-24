package dev.sunbirdrc.claim.service;

import dev.sunbirdrc.claim.entity.GeneratedNumber;
import dev.sunbirdrc.claim.repository.GeneratedNumberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CertificateNumberService {

    private final GeneratedNumberRepository numberRepository;

    @Autowired
    public CertificateNumberService(GeneratedNumberRepository numberRepository) {
        this.numberRepository = numberRepository;
    }

    public Long generateAndSaveNumber() {
        // Generate a random number (you can replace this with your logic)
        Long generatedNumber = (long) Math.floor(Math.random() * 1000000);

        // Create an entity and save it in the database
        GeneratedNumber entity = new GeneratedNumber();
        entity.setNumber(generatedNumber);
        numberRepository.save(entity);

        return generatedNumber;
    }
}
