package dev.sunbirdrc.registry.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.sunbirdrc.registry.helper.RegistryHelper;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import dev.sunbirdrc.registry.util.IDefinitionsManager;
import dev.sunbirdrc.registry.util.RefResolver;
import io.swagger.models.*;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.properties.*;
import io.swagger.util.Json;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

import static dev.sunbirdrc.registry.Constants.Schema;
import static dev.sunbirdrc.registry.Constants.TITLE;
import static dev.sunbirdrc.registry.middleware.util.Constants.ENTITY_LIST;

@RestController
@ConditionalOnProperty(name = "api-swagger.enabled", havingValue = "true")
public class RegistrySwaggerController {
    private final IDefinitionsManager definitionsManager;
    private final RefResolver refResolver;
    private final ObjectMapper objectMapper;
    @Value("${registry.schema.url}")
    private String schemaUrl;
    @Value("${api-swagger.title}")
    private String swaggerTitle;
    @Value("${api-swagger.description}")
    private String swaggerDescription;
    @Value("${api-swagger.version}")
    private String swaggerVersion;

    private RegistryHelper registryHelper;

    @Autowired
    public RegistrySwaggerController(IDefinitionsManager definitionsManager, ObjectMapper objectMapper, RefResolver refResolver, RegistryHelper registryHelper) {
        this.definitionsManager = definitionsManager;
        this.objectMapper = objectMapper;
        this.refResolver = refResolver;
        this.registryHelper = registryHelper;
    }

    @RequestMapping(value = "/api/docs/swagger.json", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<Object> getSwaggerDoc(HttpServletRequest request) throws IOException {

        ObjectNode apiDoc = generateAPIMethods(definitionsManager.getAllKnownDefinitions(), request);
        return new ResponseEntity<>(objectMapper.writeValueAsString(apiDoc), HttpStatus.OK);
    }

    private TextNode getHost(HttpServletRequest request) {
        return TextNode.valueOf(request.getRequestURL().toString()
                .replace(request.getRequestURI(), "")
                .replace(request.getScheme(), "")
                .replace("://", "")
        );
    }

    @GetMapping(value = "/api/docs/user/swagger.json", produces = "application/json")
    public ResponseEntity<Object> getUserSwaggerDoc(HttpServletRequest request) throws Exception {
        JsonNode userCreatedSchemas = registryHelper.getRequestedUserDetails(request, Schema);
        Set<String> schemas = new HashSet<>();
        for (JsonNode schemaRes : userCreatedSchemas.get(Schema).get(ENTITY_LIST)) {
            JsonNode schema = objectMapper.readTree(schemaRes.get(Schema.toLowerCase()).asText());
            schemas.add(schema.get(TITLE).textValue());
        }
        ObjectNode apiDoc = generateAPIMethods(schemas, request);
        return new ResponseEntity<>(objectMapper.writeValueAsString(apiDoc), HttpStatus.OK);
    }

    private ObjectNode generateAPIMethods(Set<String> entities, HttpServletRequest request) throws IOException {
        ObjectNode doc = (ObjectNode) objectMapper.reader().readTree(new ClassPathResource("/baseSwagger.json").getInputStream());
        ObjectNode paths = objectMapper.createObjectNode();
        ObjectNode definitions = objectMapper.createObjectNode();
        doc.set("paths", paths);
        doc.set("definitions", definitions);
        doc.set("host", getHost(request));
        doc.set("schemes", JsonNodeFactory.instance.arrayNode().add(request.getScheme()));
        doc.set("info", getApiInfo());
        for (String entityName : entities) {
            if (Character.isUpperCase(entityName.charAt(0))) {
                populateEntityActions(paths, entityName);
                populateSubEntityActions(paths, entityName);
                JsonNode schemaDefinition = objectMapper.reader().readTree(definitionsManager.getDefinition(entityName).getContent());
                deleteAll$Ids((ObjectNode) schemaDefinition);
//                definitions.set(entityName, schemaDefinition.get("definitions").get(entityName));
                for (Iterator<String> it = schemaDefinition.get("definitions").fieldNames(); it.hasNext(); ) {
                    String fieldName = it.next();
                    definitions.set(fieldName, refResolver.resolveDefinitions(fieldName, schemaDefinition.get("definitions").get(fieldName)));
                    if (schemaDefinition.get("_osConfig") != null) {
                        definitions.set(String.format("%sOsConfig", fieldName), schemaDefinition.get("_osConfig"));
                    }
                }
            }
        }
        return doc;
    }


    @GetMapping(value = "/api/docs/{file}.json", produces = "application/json")
    public ResponseEntity<Object> getSwaggerDocImportFiles(
            @PathVariable String file
    ) {
        JsonNode definitions = null;
        try {
            definitions = refResolver.getResolvedSchema(file, "properties");
        } catch (IOException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(definitions, HttpStatus.OK);
    }


    private void populateEntityActions(ObjectNode paths, String entityName) throws IOException {
        ObjectNode path = objectMapper.createObjectNode();
        PathParameter pathParam = new PathParameter()
                .name("entityId")
                .description(String.format("Id of the %s", entityName))
                .required(true)
                .type("string");
        addGetOperation(entityName, path, Collections.singletonList(pathParam));
        addModifyOperation(entityName, path, Collections.singletonList(pathParam), getBodyParameter(entityName));
        paths.set(String.format("/api/v1/%s/{entityId}", entityName), path);
        path = objectMapper.createObjectNode();
        addGetOperation("", path, Collections.emptyList());
        paths.set(String.format("/api/docs/%s.json", entityName), path);
        paths.set(String.format("/api/v1/%s/sign", entityName), path);
        path = objectMapper.createObjectNode();
        RefProperty refProperty = new RefProperty();
        refProperty.set$ref(String.format("#/definitions/%s", entityName));
        addPostOperation(entityName, path, Collections.singletonList(getSearchBodyParameter()), new ArrayModel().items(refProperty));
        paths.set(String.format("/api/v1/%s/search", entityName), path);
        path = objectMapper.createObjectNode();
        addPostOperation(entityName, path, Collections.singletonList(getBodyParameter(entityName)), new ModelImpl());
        paths.set(String.format("/api/v1/%s/invite", entityName), path);


        path = objectMapper.createObjectNode();
        addGetOperation(entityName, path, Collections.emptyList());
        addPostOperation(entityName, path, Collections.singletonList(getBodyParameter(entityName)), new RefModel(String.format("#/definitions/%s", entityName)));
        paths.set(String.format("/api/v1/%s", entityName), path);
    }

    private ModelImpl getSearchRequestModel() {
        ModelImpl searchRequest = new ModelImpl();
        Map<String, Property> searchProps = new HashMap<>();
        ObjectProperty searchField = new ObjectProperty();
        ObjectProperty searchValueObject = new ObjectProperty();
        StringProperty searchValue = new StringProperty();
        searchValue.description("Search searchField");
        searchValue.example("name");
        searchValueObject.property("operators", searchValue);
        searchValueObject.setDescription("operators can be gte, lte, contains, gt, lt, eq, between, or, startsWith, endsWith, notContains, notStartsWith, notEndsWith");
        searchField.property("field_path", searchValueObject);
        searchField.description("Ex: (field_path): $.educationDetails.name");
        searchProps.put("filters", searchField);
        IntegerProperty limitVal = new IntegerProperty();
        limitVal.setDefault(0);
        searchProps.put("limit", limitVal);
        searchProps.put("offset", limitVal);
        searchRequest.setProperties(searchProps);
        return searchRequest;
    }

    private void populateSubEntityActions(ObjectNode paths, String entityName) throws IOException {
        ObjectNode path = objectMapper.createObjectNode();
        PathParameter entityIdParam = new PathParameter()
                .name("entityId")
                .description(String.format("Id of the %s", entityName))
                .required(true)
                .type("string");
        PathParameter propertyParam = new PathParameter()
                .name("property")
                .description("Schema property field")
                .required(true)
                .type("string");
        PathParameter propertyIdParam = new PathParameter()
                .name("propertyId")
                .description("Id of schema property field")
                .required(true)
                .type("string");
        addGetOperation("", path, Arrays.asList(entityIdParam, propertyParam, propertyIdParam));
        addModifyOperation("", path, Arrays.asList(entityIdParam, propertyParam, propertyIdParam), getPropertyUpdateRequestBody());
        paths.set(String.format("/api/v1/%s/{entityId}/{property}/{propertyId}", entityName), path);
        path = objectMapper.createObjectNode();
        addPostOperation("", path, Collections.singletonList(getPropertyCreateRequestBody()), new RefModel(String.format("#/definitions/%s", "")));
        paths.set(String.format("/api/v1/%s/{entityId}/{property}", entityName), path);
        path = objectMapper.createObjectNode();
        addPostOperation("", path, Arrays.asList(entityIdParam, propertyParam, propertyIdParam), new RefModel(String.format("#/definitions/%s", "")));
        paths.set(String.format("/api/v1/%s/{entityId}/{property}/{propertyId}/send", entityName), path);
    }

    private void addModifyOperation(String entityName, ObjectNode path, List<PathParameter> pathParameters, BodyParameter bodyParameter) throws IOException {
        Operation operation = new Operation()
                .description(String.format("%s new update", entityName));
        pathParameters.forEach(operation::addParameter);
        operation.addParameter(bodyParameter);
        addResponseType(path, operation, "put", new RefModel(String.format("#/definitions/%s", entityName)));
    }

    private void addPostOperation(String entityName, ObjectNode path, List<Parameter> parameters, Model responseSchema) throws IOException {
        Operation operation = new Operation()
                .description(String.format("Create new %s", entityName));
        parameters.forEach(operation::addParameter);
        addResponseType(path, operation, "post", responseSchema);
    }

    private BodyParameter getPropertyUpdateRequestBody() {
        ModelImpl bodyParam = new ModelImpl();
        bodyParam.type("object");
        bodyParam.property("key1", new StringProperty()._default("val1"));
        bodyParam.property("send", new BooleanProperty().description("If the required property needs to be sent for attestation"));
        return new BodyParameter()
                .schema(bodyParam);
    }

    private BodyParameter getPropertyCreateRequestBody() {
        ModelImpl bodyParam = new ModelImpl();
        bodyParam.type("object");
        bodyParam.property("key1", new StringProperty()._default("val1"));
        return new BodyParameter()
                .schema(bodyParam);
    }

    private BodyParameter getBodyParameter(String entityName) {
        return new BodyParameter()
                .schema(new RefModel(String.format("#/definitions/%s", entityName)));
    }

    private BodyParameter getSearchBodyParameter() {
        return new BodyParameter()
                .schema(getSearchRequestModel());
    }

    private void addGetOperation(String entityName, ObjectNode path, List<PathParameter> pathParameters) throws IOException {
        Operation operation = new Operation();
        pathParameters.forEach(operation::addParameter);
        addResponseType(path, operation, "get", new RefModel(String.format("#/definitions/%s", entityName)));
    }

    private void addResponseType(ObjectNode path, Operation operation, String operationType, Model responseSchema) throws IOException {
        ObjectProperty schema = new ObjectProperty();
        schema.setType("object");
        io.swagger.models.Response response = new io.swagger.models.Response()
                .description("OK")
                .responseSchema(responseSchema);

        operation.addResponse("200", response);

        ObjectNode jsonOperationMapping = (ObjectNode) objectMapper.reader().readTree(Json.mapper().writeValueAsString(operation));
        deleteAll$Ids(jsonOperationMapping);
        path.set(operationType, jsonOperationMapping);
    }

    private void deleteAll$Ids(ObjectNode jsonOperationMapping) {
        if (jsonOperationMapping.has("$id"))
            jsonOperationMapping.remove("$id");
        if (jsonOperationMapping.has("$comment"))
            jsonOperationMapping.remove("$comment");
        jsonOperationMapping.forEach(x -> {
            if (x instanceof ObjectNode) {
                deleteAll$Ids((ObjectNode) x);
            }
        });
    }

    private JsonNode getApiInfo() throws IOException {
        Info info = new Info()
                .title(swaggerTitle)
                .version(swaggerVersion)
                .description(swaggerDescription);
        return JSONUtil.convertObjectJsonNode(info);
    }
}
