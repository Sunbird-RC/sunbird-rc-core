package dev.sunbirdrc.consent.service;

import dev.sunbirdrc.consent.entity.Consent;
import dev.sunbirdrc.consent.exceptions.ConsentDefinitionNotFoundException;
import dev.sunbirdrc.consent.repository.ConsentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static dev.sunbirdrc.consent.constants.ConstentStatus.GRANTED;

@Service
public class ConsentService {
    @Autowired
    private ConsentRepository consentRepository;

    public Consent saveConsent(Consent consent) {
        return consentRepository.save(consent);
    }

    public Consent retrieveConsents(String id) throws ConsentDefinitionNotFoundException {
        Consent consent = consentRepository.findById(id).orElseThrow(() -> new ConsentDefinitionNotFoundException("Invalid ID of consent"));
        return consent;
    }

    public Consent grantOrDenyConsent(String status, String id) throws Exception{
        Optional<Consent> optConsent = consentRepository.findById(id);
        Consent consent = optConsent.map(consent1 -> {
            consent1.setStatus(status.equals(GRANTED.name()));
            return consent1;
        }).orElse(null);
        if(consent == null) throw new ConsentDefinitionNotFoundException("Invalid ID of consent");
        return consentRepository.save(consent);
    }

    public List<Consent> retrieveConsentByOwnerId(String ownerId) {
        return consentRepository.findByOsOwner(ownerId);
    }
}
