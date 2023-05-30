package dev.sunbirdrc.registry.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.service.ISearchService;
import dev.sunbirdrc.registry.service.SchemaService;
import dev.sunbirdrc.registry.util.DefinitionsManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class SchemaLoaderTest {


	SchemaService schemaService = new SchemaService();

	@InjectMocks
	SchemaLoader schemaLoader;

	@Mock
	ISearchService searchService;

	ObjectMapper objectMapper = new ObjectMapper();

	DefinitionsManager definitionsManager;

	@Before
	public void setUp() throws Exception {
		definitionsManager = new DefinitionsManager();
		ReflectionTestUtils.setField(schemaLoader, "schemaService", schemaService);
		ReflectionTestUtils.setField(schemaService, "definitionsManager", definitionsManager);
	}

	@Test
	public void shouldLoadSchemasToDefinitionManager() throws IOException {
		Mockito.when(searchService.search(Mockito.any())).thenReturn(objectMapper.readTree("{\n" +
				"  \"Schema\": [\n" +
				"    {\n" +
				"      \"name\": \"DeathCertificateV2\",\n" +
				"      \"schema\": \"{\\n  \\\"$schema\\\": \\\"http://json-schema.org/draft-07/schema\\\",\\n  \\\"type\\\": \\\"object\\\",\\n  \\\"properties\\\": {\\n    \\\"DeathCertificate\\\": {\\n      \\\"$ref\\\": \\\"#/definitions/DeathCertificate\\\"\\n    }\\n  },\\n  \\\"required\\\": [\\n    \\\"DeathCertificate\\\"\\n  ],\\n  \\\"title\\\": \\\"DeathCertificate\\\",\\n  \\\"definitions\\\": {\\n    \\\"DeathCertificate\\\": {\\n      \\\"$id\\\": \\\"#/properties/DeathCertificate\\\",\\n      \\\"type\\\": \\\"object\\\",\\n      \\\"title\\\": \\\"The DeathCertificate Schema\\\",\\n      \\\"required\\\": [\\n        \\\"name\\\",\\n        \\\"gender\\\",\\n        \\\"date_of_death\\\",\\n        \\\"place_of_death\\\",\\n        \\\"date_of_registration\\\",\\n        \\\"registration_no\\\",\\n        \\\"name_of_mother\\\",\\n        \\\"name_of_father\\\",\\n        \\\"name_of_spouse\\\",\\n        \\\"present_address\\\",\\n        \\\"permanent_address\\\"\\n      ],\\n      \\\"properties\\\": {\\n        \\\"name\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"gender\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"date_of_death\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"place_of_death\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"date_of_registration\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"registration_no\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"name_of_mother\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"name_of_father\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"name_of_spouse\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"present_address\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"permanent_address\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        }\\n      }\\n    }\\n  },\\n  \\\"_osConfig\\\": {\\n    \\\"uniqueIndexFields\\\": [\\n      \\\"contact\\\"\\n    ],\\n    \\\"ownershipAttributes\\\": [],\\n    \\\"roles\\\": [],\\n    \\\"inviteRoles\\\": [\\n      \\\"anonymous\\\"\\n    ],\\n    \\\"enableLogin\\\": false,\\n    \\\"credentialTemplate\\\": {\\n      \\\"@context\\\": [\\n        \\\"https://www.w3.org/2018/credentials/v1\\\",\\n        {\\n          \\\"@context\\\": {\\n            \\\"@version\\\": 1.1,\\n            \\\"@protected\\\": true,\\n            \\\"DeathCertificate\\\": {\\n              \\\"@id\\\": \\\"https://github.com/sunbird-specs/vc-specs#DeathCertificate\\\",\\n              \\\"@context\\\": {\\n                \\\"id\\\": \\\"@id\\\",\\n                \\\"@version\\\": 1.1,\\n                \\\"@protected\\\": true,\\n                \\\"skills\\\": \\\"schema:Text\\\"\\n              }\\n            },\\n            \\\"Person\\\": {\\n              \\\"@id\\\": \\\"https://github.com/sunbird-specs/vc-specs#Person\\\",\\n              \\\"@context\\\": {\\n                \\\"name\\\": \\\"schema:Text\\\",\\n                \\\"gender\\\": \\\"schema:Text\\\",\\n                \\\"date_of_death\\\": \\\"schema:Text\\\",\\n                \\\"place_of_death\\\": \\\"schema:Text\\\",\\n                \\\"date_of_registration\\\": \\\"schema:Text\\\",\\n                \\\"registration_no\\\": \\\"schema:Text\\\",\\n                \\\"name_of_mother\\\": \\\"schema:Text\\\",\\n                \\\"name_of_father\\\": \\\"schema:Text\\\",\\n                \\\"name_of_spouse\\\": \\\"schema:Text\\\",\\n                \\\"present_address\\\": \\\"schema:Text\\\",\\n                \\\"permanent_address\\\": \\\"schema:Text\\\"\\n              }\\n            },\\n            \\\"trainedOn\\\": {\\n              \\\"@id\\\": \\\"https://github.com/sunbird-specs/vc-specs#trainedOn\\\",\\n              \\\"@context\\\": {\\n                \\\"name\\\": \\\"schema:Text\\\"\\n              }\\n            }\\n          }\\n        }\\n      ],\\n      \\\"type\\\": [\\n        \\\"VerifiableCredential\\\"\\n      ],\\n      \\\"issuanceDate\\\": \\\"2021-08-27T10:57:57.237Z\\\",\\n      \\\"credentialSubject\\\": {\\n        \\\"type\\\": \\\"Person\\\",\\n        \\\"name\\\": \\\"{{name}}\\\",\\n        \\\"gender\\\": \\\"{{gender}}\\\",\\n        \\\"date_of_death\\\": \\\"{{date_of_death}}\\\",\\n        \\\"place_of_death\\\": \\\"{{place_of_death}}\\\",\\n        \\\"date_of_registration\\\": \\\"{{date_of_registration}}\\\",\\n        \\\"registration_no\\\": \\\"{{registration_no}}\\\",\\n        \\\"name_of_mother\\\": \\\"{{name_of_mother}}\\\",\\n        \\\"name_of_father\\\": \\\"{{name_of_father}}\\\",\\n        \\\"name_of_spouse\\\": \\\"{{name_of_spouse}}\\\",\\n        \\\"present_address\\\": \\\"{{present_address}}\\\",\\n        \\\"permanent_address\\\": \\\"{{permanent_address}}\\\"\\n      },\\n      \\\"issuer\\\": \\\"did:web:sunbirdrc.dev/vc/skill\\\"\\n    },\\n    \\\"certificateTemplates\\\": {\\n      \\\"html\\\": \\\"https://gist.githubusercontent.com/snehalmadakatti/b2179a3e6c6a6101bfabc92a632a57ad/raw/efbbe82f9e582b260e06acedbb0c6318cc04cb2b/deathcertificate.html\\\",\\n      \\\"svg\\\": \\\"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/DeathCertificate.svg\\\"\\n    }\\n  }\\n}\",\n" +
				"      \"status\": \"PUBLISHED\",\n" +
				"      \"osOwner\": [\n" +
				"        \"d9e68be4-205a-4b44-8301-1fea2557f1cf\"\n" +
				"      ],\n" +
				"      \"osCreatedAt\": \"2022-12-16T11:12:23.347Z\",\n" +
				"      \"osUpdatedAt\": \"2022-12-16T11:12:23.347Z\",\n" +
				"      \"osCreatedBy\": \"d9e68be4-205a-4b44-8301-1fea2557f1cf\",\n" +
				"      \"osUpdatedBy\": \"d9e68be4-205a-4b44-8301-1fea2557f1cf\",\n" +
				"      \"osid\": \"1-e6042101-c6c7-4a62-a448-68e663b0c3c9\"\n" +
				"    }\n" +
				"  ]\n" +
				"}"));
		schemaLoader.onApplicationEvent(new ContextRefreshedEvent(new GenericApplicationContext()));
		assertEquals(1, definitionsManager.getAllDefinitions().size());
	}
}
