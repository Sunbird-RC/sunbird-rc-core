package dev.sunbirdrc.validators.json.jsonschema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dev.sunbirdrc.registry.middleware.MiddlewareHaltException;
import dev.sunbirdrc.registry.util.Definition;
import org.junit.Before;
import org.junit.Test;

import java.io.*;

public class JsonValidationServiceImplTest {

    private JsonValidationServiceImpl jsonValidationService;

    private static Definition schemaDefinition;
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String schemaDirectory = "classpath://validation-test/_schemas/";

    private static final String sampleSchemaPath = "src/test/resources/validation-test/_schemas/Student.json";
    private static final String sampleJsonPathOnlyRequiredErrors = "src/test/resources/validation-test/records/student1.json";
    private static final String sampleJsonPathRequiredSchemaErrors = "src/test/resources/validation-test/records/student2.json";

    private JsonNode jsonObj;

    @Before
    public void setUp() throws IOException {
        schemaDefinition = new Definition(mapper.readTree(new File(sampleSchemaPath)));


        jsonValidationService = new JsonValidationServiceImpl(schemaDirectory);
        jsonValidationService.addDefinitions(schemaDefinition.getTitle(), schemaDefinition.getContent());
    }

    @Test(expected = MiddlewareHaltException.class)
    public void testValidate() throws Exception {
        jsonObj = mapper.readTree(new File(sampleJsonPathOnlyRequiredErrors));
        jsonValidationService.validate(schemaDefinition.getTitle(), mapper.writeValueAsString(jsonObj), false);
    }

    @Test
    public void testIgnoreRequiredValidation() throws Exception {
        jsonObj = mapper.readTree(new File(sampleJsonPathOnlyRequiredErrors));
        jsonValidationService.validate(schemaDefinition.getTitle(), mapper.writeValueAsString(jsonObj), true);
    }

    @Test(expected = MiddlewareHaltException.class)
    public void testIgnoreRequiredValidationWithSchemaViolations() throws Exception {
        jsonObj = mapper.readTree(new File(sampleJsonPathRequiredSchemaErrors));
        jsonValidationService.validate(schemaDefinition.getTitle(), mapper.writeValueAsString(jsonObj), true);
    }

    @Test(expected = MiddlewareHaltException.class)
    public void shouldAddSchemaAndValidateAndThrowError() throws Exception {
        JsonNode jsonNode = JsonNodeFactory.instance.textNode("{\n  \"$schema\": \"http://json-schema.org/draft-07/schema\",\n  \"type\": \"object\",\n  \"properties\": {\n    \"TrainingCertificate\": {\n      \"$ref\": \"#/definitions/TrainingCertificate\"\n    }\n  },\n  \"required\": [\n    \"TrainingCertificate\"\n  ],\n  \"title\": \"TrainingCertificate\",\n  \"definitions\": {\n    \"TrainingCertificate\": {\n      \"$id\": \"#/properties/TrainingCertificate\",\n      \"type\": \"object\",\n      \"title\": \"The TrainingCertificate Schema\",\n      \"required\": [\n        \"name\",\n        \"contact\"\n      ],\n      \"properties\": {\n        \"name\": {\n          \"type\": \"string\"\n        },\n        \"trainingTitle\": {\n          \"type\": \"string\"\n        },\n        \"contact\": {\n          \"type\": \"string\"\n        },\n        \"date\": {\n          \"type\": \"string\",\n          \"format\": \"date\"\n        },\n        \"note\": {\n          \"type\": \"string\"\n        }\n      }\n    }\n  },\n  \"_osConfig\": {\n    \"uniqueIndexFields\": [\n      \"contact\"\n    ],\n    \"ownershipAttributes\": [],\n    \"roles\": [],\n    \"inviteRoles\": [\n      \"anonymous\"\n    ],\n    \"enableLogin\": false,\n    \"credentialTemplate\": {\n      \"@context\": [\n        \"https://www.w3.org/2018/credentials/v1\",\n        \"https://gist.githubusercontent.com/dileepbapat/eb932596a70f75016411cc871113a789/raw/498e5af1d94784f114b32c1ab827f951a8a24def/skill\"\n      ],\n      \"type\": [\n        \"VerifiableCredential\"\n      ],\n      \"issuanceDate\": \"2021-08-27T10:57:57.237Z\",\n      \"credentialSubject\": {\n        \"type\": \"Person\",\n        \"name\": \"{{name}}\",\n        \"trainedOn\": \"{{trainingTitle}}\"\n      },\n      \"issuer\": \"did:web:sunbirdrc.dev/vc/skill\"\n    },\n    \"certificateTemplates\": {\n      \"html\": \"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.html\",\n      \"svg\": \"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.svg\"\n    }\n  }\n}");
        jsonValidationService.addDefinitions(jsonNode);
        jsonValidationService.validate("TrainingCertificate", "{\"TrainingCertificate\":{\"trainingTitle\": \"Certificate Module\"}}", false);

    }

    @Test
    public void shouldAddSchemaAndValidateWithoutError() throws Exception {
        JsonNode jsonNode = JsonNodeFactory.instance.textNode("{\n  \"$schema\": \"http://json-schema.org/draft-07/schema\",\n  \"type\": \"object\",\n  \"properties\": {\n    \"TrainingCertificate\": {\n      \"$ref\": \"#/definitions/TrainingCertificate\"\n    }\n  },\n  \"required\": [\n    \"TrainingCertificate\"\n  ],\n  \"title\": \"TrainingCertificate\",\n  \"definitions\": {\n    \"TrainingCertificate\": {\n      \"$id\": \"#/properties/TrainingCertificate\",\n      \"type\": \"object\",\n      \"title\": \"The TrainingCertificate Schema\",\n      \"required\": [\n        \"name\",\n        \"contact\"\n      ],\n      \"properties\": {\n        \"name\": {\n          \"type\": \"string\"\n        },\n        \"trainingTitle\": {\n          \"type\": \"string\"\n        },\n        \"contact\": {\n          \"type\": \"string\"\n        },\n        \"date\": {\n          \"type\": \"string\",\n          \"format\": \"date\"\n        },\n        \"note\": {\n          \"type\": \"string\"\n        }\n      }\n    }\n  },\n  \"_osConfig\": {\n    \"uniqueIndexFields\": [\n      \"contact\"\n    ],\n    \"ownershipAttributes\": [],\n    \"roles\": [],\n    \"inviteRoles\": [\n      \"anonymous\"\n    ],\n    \"enableLogin\": false,\n    \"credentialTemplate\": {\n      \"@context\": [\n        \"https://www.w3.org/2018/credentials/v1\",\n        \"https://gist.githubusercontent.com/dileepbapat/eb932596a70f75016411cc871113a789/raw/498e5af1d94784f114b32c1ab827f951a8a24def/skill\"\n      ],\n      \"type\": [\n        \"VerifiableCredential\"\n      ],\n      \"issuanceDate\": \"2021-08-27T10:57:57.237Z\",\n      \"credentialSubject\": {\n        \"type\": \"Person\",\n        \"name\": \"{{name}}\",\n        \"trainedOn\": \"{{trainingTitle}}\"\n      },\n      \"issuer\": \"did:web:sunbirdrc.dev/vc/skill\"\n    },\n    \"certificateTemplates\": {\n      \"html\": \"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.html\",\n      \"svg\": \"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.svg\"\n    }\n  }\n}");
        jsonValidationService.addDefinitions(jsonNode);
        jsonValidationService.validate("TrainingCertificate", "{\n" +
                "  \"TrainingCertificate\": {\n" +
                "    \"trainingTitle\": \"Certificate Module\",\n" +
                "    \"name\": \"abc\",\n" +
                "    \"contact\": \"123\"\n" +
                "  }\n" +
                "}", false);

    }

    @Test
    public void shouldTestSchemaValidations() throws Exception {
        JsonNode jsonNode = JsonNodeFactory.instance.textNode("{\n" +
                "  \"$schema\": \"http://json-schema.org/draft-07/schema\",\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"TrainingCertificate\": {\n" +
                "      \"$ref\": \"#/definitions/TrainingCertificate\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"required\": [\n" +
                "    \"TrainingCertificate\"\n" +
                "  ],\n" +
                "  \"title\": \"TrainingCertificate\",\n" +
                "  \"definitions\": {\n" +
                "    \"TrainingCertificate\": {\n" +
                "      \"$id\": \"#/properties/TrainingCertificate\",\n" +
                "      \"type\": \"object\",\n" +
                "      \"title\": \"The TrainingCertificate Schema\",\n" +
                "      \"required\": [\n" +
                "        \"name\",\n" +
                "        \"contact\"\n" +
                "      ],\n" +
                "      \"additionalProperties\": false,\n" +
                "      \"properties\": {\n" +
                "        \"name\": {\n" +
                "          \"type\": \"string\",\n" +
                "          \"enum\": [\"A\",\"B\"]\n" +
                "        },\n" +
                "        \"trainingTitle\": {\n" +
                "          \"type\": \"string\",\n" +
                "          \"const\": \"ABC\" \n" +
                "        },\n" +
                "        \"contact\": {\n" +
                "          \"type\": \"number\",\n" +
                "          \"multipleOf\": 3,\n" +
                "          \"maximum\": 99,\n" +
                "          \"minimum\": 9\n" +
                "        },\n" +
                "        \"date\": {\n" +
                "          \"type\": \"string\",\n" +
                "          \"format\": \"date\"\n" +
                "        },\n" +
                "        \"note\": {\n" +
                "          \"type\": \"string\",\n" +
                "          \"maxLength\": 10,\n" +
                "          \"minLength\": 5\n" +
                "        },\n" +
                "        \"items\": {\n" +
                "          \"type\": \"array\",\n" +
                "          \"maxItems\": 3,\n" +
                "          \"minItems\": 1,\n" +
                "          \"uniqueItems\": true,\n" +
                "          \"items\": {\n" +
                "            \"type\": \"string\"\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"_osConfig\": {\n" +
                "    \"uniqueIndexFields\": [\n" +
                "      \"contact\"\n" +
                "    ],\n" +
                "    \"ownershipAttributes\": [],\n" +
                "    \"roles\": [],\n" +
                "    \"inviteRoles\": [\n" +
                "      \"anonymous\"\n" +
                "    ],\n" +
                "    \"enableLogin\": false,\n" +
                "    \"credentialTemplate\": {\n" +
                "      \"@context\": [\n" +
                "        \"https://www.w3.org/2018/credentials/v1\",\n" +
                "        \"https://gist.githubusercontent.com/dileepbapat/eb932596a70f75016411cc871113a789/raw/498e5af1d94784f114b32c1ab827f951a8a24def/skill\"\n" +
                "      ],\n" +
                "      \"type\": [\n" +
                "        \"VerifiableCredential\"\n" +
                "      ],\n" +
                "      \"issuanceDate\": \"2021-08-27T10:57:57.237Z\",\n" +
                "      \"credentialSubject\": {\n" +
                "        \"type\": \"Person\",\n" +
                "        \"name\": \"{{name}}\",\n" +
                "        \"trainedOn\": \"{{trainingTitle}}\"\n" +
                "      },\n" +
                "      \"issuer\": \"did:web:sunbirdrc.dev/vc/skill\"\n" +
                "    },\n" +
                "    \"certificateTemplates\": {\n" +
                "      \"html\": \"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.html\",\n" +
                "      \"svg\": \"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.svg\"\n" +
                "    }\n" +
                "  }\n" +
                "}");
        jsonValidationService.addDefinitions(jsonNode);
        jsonValidationService.validate("TrainingCertificate", "{\n" +
                "  \"TrainingCertificate\": {\n" +
                "    \"trainingTitle\": \"ABC\",\n" +
                "    \"name\": \"A\",\n" +
                "    \"date\": \"2022-10-20\",\n" +
                "    \"note\": \"12345\",\n" +
                "    \"items\": [\"1\",\"@\"],\n" +
                "    \"contact\": 99\n" +
                "  }\n" +
                "}", false);

    }
}
