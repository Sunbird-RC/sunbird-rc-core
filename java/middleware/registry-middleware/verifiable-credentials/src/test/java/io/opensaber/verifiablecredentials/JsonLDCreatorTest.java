package io.opensaber.verifiablecredentials;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class JsonLDCreatorTest {
    JsonLDCreator jsonLDCreator;

    @Test
    public void shouldAbleToGenerateTheTemplate() {
        String data = "{\"program\":\"oh on\",\"graduationYear\":\"2021\",\"marks\":\"78\",\"institute\":\"b62b3d52-cffe-428d-9dd1-61ba7b0a5882\",\"documents\":[{\"fileName\":\"e3266115-0bd0-4456-a347-96f4dc335761-blog_draft\",\"format\":\"file\"},{\"fileName\":\"e56dab1b-bd92-41bb-b9e5-e991438f27b8-NDEAR.txt\",\"format\":\"file\"}]}";
        String issuer = "did:issuer:MySystem";
        jsonLDCreator = new JsonLDCreator(data, issuer);
        String date = jsonLDCreator.getToday();
        String expectedValue = "{\"@context\":{\"schema\":\"https://schema.org/\",\"data\":\"schema:name\",\"issuer\":\"schema:url\",\"date\":\"schema:date\"},\"data\":\"{\\\"program\\\":\\\"oh on\\\",\\\"graduationYear\\\":\\\"2021\\\",\\\"marks\\\":\\\"78\\\",\\\"institute\\\":\\\"b62b3d52-cffe-428d-9dd1-61ba7b0a5882\\\",\\\"documents\\\":[{\\\"fileName\\\":\\\"e3266115-0bd0-4456-a347-96f4dc335761-blog_draft\\\",\\\"format\\\":\\\"file\\\"},{\\\"fileName\\\":\\\"e56dab1b-bd92-41bb-b9e5-e991438f27b8-NDEAR.txt\\\",\\\"format\\\":\\\"file\\\"}]}\",\"issuer\":\"did:issuer:MySystem\",\"date\":\"" + date + "\"}";
        assertEquals(expectedValue, jsonLDCreator.getValue());
    }
}