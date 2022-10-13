package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.registry.entities.RevokedCredential;
import dev.sunbirdrc.registry.helper.RegistryHelper;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.service.RegistryService;
import dev.sunbirdrc.workflow.KieConfiguration;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
@SpringBootTest(classes = {ObjectMapper.class, KieConfiguration.class})
public class RevocationServiceImplTest {

	@Mock
	RegistryHelper registryHelper;
	@InjectMocks
	@Spy
	RevocationServiceImpl revocationServiceImpl;

	@Test
	public void shouldAddCredentialToRegistry() throws Exception {
		String entity = "TrainingCertificate";
		String sign = "{\n" +
				"    \"@context\": [\n" +
				"        \"https://www.w3.org/2018/credentials/v1\",\n" +
				"        {\n" +
				"            \"@context\": {\n" +
				"                \"@version\": 1.1,\n" +
				"                \"@protected\": true,\n" +
				"                \"Beneficiary\": {\n" +
				"                    \"@id\": \"https://github.com/sunbird-specs/vc-specs#Beneficiary\",\n" +
				"                    \"@context\": {\n" +
				"                        \"id\": \"@id\",\n" +
				"                        \"@version\": 1.1,\n" +
				"                        \"@protected\": true,\n" +
				"                        \"name\": \"schema:Text\",\n" +
				"                        \"dob\": \"schema:Text\",\n" +
				"                        \"gender\": \"schema:Text\",\n" +
				"                        \"phone\": \"schema:Text\",\n" +
				"                        \"account_number\": \"schema:Text\",\n" +
				"                        \"bank_name\": \"schema:Text\",\n" +
				"                        \"disability_level\": \"schema:Text\",\n" +
				"                        \"program\": \"schema:Text\"\n" +
				"                    }\n" +
				"                },\n" +
				"                \"eligibility\": {\n" +
				"                    \"@id\": \"https://github.com/sunbird-specs/vc-specs#eligibility\",\n" +
				"                    \"@context\": {\n" +
				"                        \"id\": \"@id\",\n" +
				"                        \"@version\": 1.1,\n" +
				"                        \"@protected\": true,\n" +
				"                        \"isEligible\": \"schema:Text\"\n" +
				"                    }\n" +
				"                }\n" +
				"            }\n" +
				"        }\n" +
				"    ],\n" +
				"    \"type\": [\n" +
				"        \"VerifiableCredential\"\n" +
				"    ],\n" +
				"    \"issuanceDate\": \"2021-08-27T10:57:57.237Z\",\n" +
				"    \"credentialSubject\": {\n" +
				"        \"type\": \"Beneficiary\",\n" +
				"        \"name\": \"Ade Hastuti\",\n" +
				"        \"dob\": \"2022-08-01\",\n" +
				"        \"gender\": \"Female\",\n" +
				"        \"phone\": \"+62-12-571-0072\",\n" +
				"        \"account_number\": \"229899429\",\n" +
				"        \"bank_name\": \"slcb\",\n" +
				"        \"disability_level\": \"58\",\n" +
				"        \"program\": \"flood_relief_aug_2022\",\n" +
				"        \"eligibility\": {\n" +
				"            \"isEligible\": \"false\"\n" +
				"        }\n" +
				"    },\n" +
				"    \"issuer\": \"did:web:openg2p\",\n" +
				"    \"proof\": {\n" +
				"        \"type\": \"RsaSignature2018\",\n" +
				"        \"created\": \"2022-09-29T12:18:10Z\",\n" +
				"        \"verificationMethod\": \"did:india\",\n" +
				"        \"proofPurpose\": \"assertionMethod\",\n" +
				"        \"jws\": \"eyJhbGciOiJQUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..eOwitA6GhXXQxGkpaQ8LbRwtApBRrpQ-Bsx1SftBacX4X4l1uV1nDgjys0rGp6RpwL0rF2yksGnYH-jAC18T1kvR1xBUvmFfTu3lTDirPR4WbyuFhAgb6hU-84NPlBb_GHTmO5IWaIe_q5GT7joLfsYHACGYKc05B5Su_PXNt4ETJ3_tIPljxs8EpqAr8zaX3g2q9KPaOEdrFAqjoH4adBKbhxv3KDlth-zZSeG0Gi4LQuPXNk7K_no5_hJJ5VBd8fk63wKpQmZAoUptf-B-eo2-Yel4noBEWGC8pth7LVIBonjQrv4OX7rNh_dDx9RcadtDQyImH0Nxgyy8G0IB7A\"\n" +
				"    }\n" +
				"}";
		System.out.println(RevokedCredential.builder().entity("Student").build().hashCode());
		System.out.println(RevokedCredential.builder().entity("Student").build().hashCode());
		String entityOsid = "1-asd-dasd";
		String signedData = "";
		String attestaionOsid = "";
		String attestation = "";
		String userId = "";
		when(registryHelper.addEntity(any(), any())).thenReturn(UUID.randomUUID().toString());
		Vertex deletedVertex = mock(Vertex.class);
//		when(deletedVertex.property())
		revocationServiceImpl.storeCredential("Student",entityOsid, userId, deletedVertex);
		verify(registryHelper, times(1)).addEntity(any(), any());
	}

}