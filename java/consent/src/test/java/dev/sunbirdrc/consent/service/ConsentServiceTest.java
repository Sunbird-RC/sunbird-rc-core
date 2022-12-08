package dev.sunbirdrc.consent.service;

import dev.sunbirdrc.consent.entity.Consent;
import dev.sunbirdrc.consent.exceptions.ConsentDefinitionNotFoundException;
import dev.sunbirdrc.consent.repository.ConsentRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static dev.sunbirdrc.consent.constants.ConstentStatus.GRANTED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ConsentServiceTest {
    @Mock
    private ConsentRepository consentRepository;
    @InjectMocks
    private ConsentService consentService;

    @Test
    public void shouldCallSaveMethodInConsentRepository() {
        Consent consent = new Consent();
        List<String> osOwners = new ArrayList<>();
        osOwners.add("789");
        consent.setEntityName("Teacher");
        consent.setEntityId("123");
        consent.setRequestorName("Institute");
        consent.setRequestorId("456");
        HashMap map = new HashMap();
        map.put("name", 1);
        consent.setConsentFields(map);
        consent.setExpirationTime("1000");
        consent.setOsOwner(osOwners);
        consentService.saveConsent(consent);
        verify(consentRepository, times(1)).save(consent);
    }

    @Test
    public void shouldRetrieveConsentsBasedOnId() throws ConsentDefinitionNotFoundException {
        Consent expectedConsent = new Consent();
        List<String> osOwners = new ArrayList<>();
        osOwners.add("789");
        expectedConsent.setEntityName("Teacher");
        expectedConsent.setEntityId("123");
        expectedConsent.setRequestorName("Institute");
        expectedConsent.setRequestorId("456");
        HashMap map = new HashMap();
        map.put("name", 1);
        expectedConsent.setConsentFields(map);
        expectedConsent.setExpirationTime("1000");
        expectedConsent.setOsOwner(osOwners);
        when(consentRepository.findById("123")).thenReturn(Optional.of(expectedConsent));
        Consent actualConsent = consentService.retrieveConsents("123");
        verify(consentRepository, times(1)).findById("123");
        assertEquals(expectedConsent, actualConsent);
    }

    @Test
    public void shouldThrowExceptionIfConsentIsNotAvailableForId() {
        when(consentRepository.findById("123")).thenReturn(Optional.ofNullable(null));
        assertThrows(ConsentDefinitionNotFoundException.class, () -> consentService.retrieveConsents("123"));
    }

    @Test
    public void shouldGrantConsent() throws Exception {
        Consent consent = new Consent();
        List<String> osOwners = new ArrayList<>();
        osOwners.add("789");
        consent.setEntityName("Teacher");
        consent.setEntityId("123");
        consent.setRequestorName("Institute");
        consent.setRequestorId("456");
        HashMap map = new HashMap();
        map.put("name", 1);
        consent.setConsentFields(map);
        consent.setExpirationTime("1000");
        consent.setOsOwner(osOwners);
        when(consentRepository.findById("123")).thenReturn(Optional.of(consent));
        consentService.grantOrDenyConsent(GRANTED.name(), "123");
        consent.setStatus(true);
        verify(consentRepository, times(1)).findById("123");
        verify(consentRepository, times(1)).save(consent);
    }

    @Test
    public void shouldRetrieveConsentByOwnerId() {
        Consent consent = new Consent();
        List<String> osOwners = new ArrayList<>();
        osOwners.add("789");
        consent.setEntityName("Teacher");
        consent.setEntityId("123");
        consent.setRequestorName("Institute");
        consent.setRequestorId("456");
        HashMap map = new HashMap();
        map.put("name", 1);
        consent.setConsentFields(map);
        consent.setExpirationTime("1000");
        consent.setOsOwner(osOwners);
        List<Consent> consents = new ArrayList<>();
        consents.add(consent);
        when(consentRepository.findByOsOwner("789")).thenReturn(consents);
        consentService.retrieveConsentByOwnerId("789");
        verify(consentRepository, times(1)).findByOsOwner("789");

    }
}
