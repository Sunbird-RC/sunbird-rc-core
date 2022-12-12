package dev.sunbirdrc.consent.service;

import dev.sunbirdrc.consent.entity.Consent;
import dev.sunbirdrc.consent.exceptions.ConsentDefinitionNotFoundException;
import dev.sunbirdrc.consent.exceptions.ConsentForbiddenException;
import dev.sunbirdrc.consent.repository.ConsentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
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

    public Consent retrieveConsents(String id, String requestorId) throws ConsentDefinitionNotFoundException, ConsentForbiddenException {
        Consent consent = consentRepository.findById(id)
                                .orElseThrow(() -> new ConsentDefinitionNotFoundException("Invalid ID of consent"));
        boolean isGranted = consent.isStatus();
        boolean isOwner = requestorId.equals(consent.getRequestorId());
        if(!isOwner) {
            final String forbidden = "You are not authorized to access this consent";
            throw new ConsentForbiddenException(forbidden);
        }
        if(!isGranted) {
            throw new ConsentForbiddenException("Consent denied or not approved until now");
        }
        if(isConsentTimeExpired(consent.getCreatedAt(), consent.getExpirationTime())) {
            final String consentTimeExpired = "Consent Time Expired";
            throw new ConsentForbiddenException(consentTimeExpired);
        }
        return consent;
    }

    private boolean isConsentTimeExpired(Date createdAt, String expirationTime) {
        Date expirationAt = new Date();
        Date currentDate = new Date();
        expirationAt.setTime(createdAt.getTime() + Long.parseLong(expirationTime) * 1000);
        return expirationAt.compareTo(currentDate) < 0;
    }

    public Consent grantOrDenyConsent(String status, String id, String consenterId) throws Exception{
        Optional<Consent> optConsent = consentRepository.findById(id);
        Consent consent = optConsent.map(consent1 -> {
            if(consent1.getOsOwner().contains(consenterId)) {
                consent1.setStatus(status.equals(GRANTED.name()));
                return consent1;
            }
            try {
                throw new ConsentForbiddenException("You are not authorized to update this consent");
            } catch (ConsentForbiddenException e) {
                throw new RuntimeException(e);
            }
        }).orElse(null);
        if(consent == null) throw new ConsentDefinitionNotFoundException("Invalid ID of consent");

        return consentRepository.save(consent);
    }

    public List<Consent> retrieveConsentByOwnerId(String ownerId) {
        return consentRepository.findByOsOwner(ownerId);
    }
}
