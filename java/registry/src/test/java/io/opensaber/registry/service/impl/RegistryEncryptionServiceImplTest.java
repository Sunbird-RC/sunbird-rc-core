package io.opensaber.registry.service.impl;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import java.nio.charset.Charset;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.service.EncryptionService;

@RunWith(SpringRunner.class)
@SpringBootTest(classes={EncryptionServiceImpl.class})
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class RegistryEncryptionServiceImplTest {

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();
	
	@Value("${encryption.active}")
	private String encryptionStatus;
	
	@Value("${decryption.active}")
	private String decryptionStatus;
	
	
	@Autowired
	private EncryptionService encryptionService;
	
	@Test
	public void test_service_status() throws Exception {		
		 byte[] array = new byte[7];
		 new Random().nextBytes(array);
		 String generatedString = new String(array, Charset.forName("UTF-8"));
		 ResponseEntity<String> response = encryptionService.encrypt(generatedString);	   
		 assertNotEquals(HttpStatus.SERVICE_UNAVAILABLE,response.getStatusCode());		
	}

	@Test
	public void test_service_encryption() throws Exception {
	
		 byte[] array = new byte[7];
		 new Random().nextBytes(array);
		 String generatedString = new String(array, Charset.forName("UTF-8"));
		 ResponseEntity<String> response = encryptionService.encrypt(generatedString);
		 
		 assertNotEquals(generatedString,response.getBody());
		 assertNotEquals(null,response.getBody());
		 
	}
	@Test
	public void test_null_value_for_encryption() throws Exception {
	
		 byte[] array = new byte[7];
		 new Random().nextBytes(array);
		 String generatedString = new String(array, Charset.forName("UTF-8"));
		 ResponseEntity<String> response = encryptionService.encrypt(generatedString);
		 if(response==null) {
		 expectedEx.expect(NullPointerException.class);
		 expectedEx.expectMessage(containsString("encrypted value cannot be null!"));
		 }
		 assertNotEquals(null,response.getBody());		 
	}
	@Test
	public void test_service_decryption() throws Exception {

		 byte[] array = new byte[7];
		 new Random().nextBytes(array);
		 String generatedString = new String(array, Charset.forName("UTF-8"));
		 ResponseEntity<String> encryptedValue = encryptionService.encrypt(generatedString);
		 ResponseEntity<String> decryptedValue = encryptionService.decrypt(encryptedValue.getBody());
		 
		 assertEquals(generatedString,decryptedValue.getBody());
		 assertNotEquals(null,decryptedValue.getBody());
	}
	
	@Test
	public void test_null_value_for_decryption() throws Exception {
		 byte[] array = new byte[7];
		 new Random().nextBytes(array);
		 String generatedString = new String(array, Charset.forName("UTF-8"));
		 ResponseEntity<String> encryptedValue = encryptionService.encrypt(generatedString);
		 ResponseEntity<String> decryptedValue = encryptionService.decrypt(encryptedValue.getBody());
		 
		 if(decryptedValue==null) {
			 expectedEx.expect(NullPointerException.class);
			 expectedEx.expectMessage(containsString("decrypted value cannot be null!"));
			 }
		 assertNotEquals(null,decryptedValue.getBody());
	}
	@Test
	public void test_only_encrypted_value_decryption() throws Exception {
		 byte[] array = new byte[7];
		 new Random().nextBytes(array);
		 String generatedString = new String(array, Charset.forName("UTF-8"));	
		 ResponseEntity<String> decryptedValue = encryptionService.decrypt(generatedString);	
		 if(decryptedValue==null) {
		 expectedEx.expect(EncryptionException.class);
		 expectedEx.expectMessage(containsString("no encrypted value for decryption!"));
		 }
	}
	
}
