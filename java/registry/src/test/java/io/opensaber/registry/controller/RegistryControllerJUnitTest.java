package io.opensaber.registry.controller;
//package io.opensaber.registry.controller;
//
//import static org.junit.Assert.*;
//
//import org.junit.FixMethodOrder;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.junit.runners.MethodSorters;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.test.context.junit4.SpringRunner;
//import org.springframework.web.client.RestTemplate;
//
//import com.fasterxml.jackson.databind.node.ObjectNode;
//
//import io.opensaber.registry.util.JsonKeys;
//
//@RunWith(SpringRunner.class)
//@SpringBootTest(classes=RegistryController.class)
//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//public class RegistryControllerTest {
//
//	@Test
//	public void testConvertToRdf() {
//		RestTemplate restTemplate = new RestTemplate();
//		HttpHeaders headers = new HttpHeaders();
//		headers.setContentType(MediaType.APPLICATION_JSON);
//		HttpEntity entity = new HttpEntity(getJsonString(),headers);
//		ResponseEntity response = restTemplate.postForEntity(generateUrl("/convertToRdf"),
//				entity,ObjectNode.class);
//		ObjectNode obj = (ObjectNode)response.getBody();
//		assertTrue(obj.get(JsonKeys.RESPONSE).asText().equals(JsonKeys.SUCCESS));
//	}
//	
//	public String generateUrl(String url){
//		return "http://localhost:8080"+url;
//	}
//	
//	public String getJsonString(){
//		return "{\"@context\": {\"schema\": \"http://schema.org/\",\"opensaber\": \"http://open-saber.org/vocab/core/#\"},\"@type\": "
//				+ "[\"schema:Person\",\"opensabre:Teacher\"],\"schema:identifier\": \"b6ad2941-fac3-4c72-94b7-eb638538f55f\",\"schema:image\": null,"
//				+ "\"schema:nationality\": \"Indian\",\"schema:birthDate\": \"2011-12-06\",\"schema:name\": \"Marvin\",\"schema:gender\": \"male\","
//				+ "\"schema:familyName\":\"Pande\",\"opensaber:languagesKnownISO\": [\"en\",\"hi\"]}";
//	}
//	
//	
//	@Test
//	public void testRetrievJsonld() {
//		RestTemplate restTemplate = new RestTemplate();
//		ResponseEntity response = restTemplate.getForEntity(generateUrl("/retrieveJsonld"),ObjectNode.class);
//		ObjectNode obj = (ObjectNode)response.getBody();
//		System.out.println("Response:"+obj.toString());
//		assertTrue(obj.get(JsonKeys.RESPONSE).asText().equals(JsonKeys.SUCCESS));
//	}
//
//}
