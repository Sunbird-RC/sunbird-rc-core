package io.opensaber.registry.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.registry.util.DefinitionsManager;
import io.opensaber.registry.util.RefResolver;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.RefModel;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.properties.BooleanProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.util.Json;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@RestController
public class RegistrySwaggerController {
    private final DefinitionsManager definitionsManager;
    private final RefResolver refResolver;
    private final ObjectMapper objectMapper;
    @Value("${registry.schema.url}")
    private String schemaUrl;

    @Autowired
    public RegistrySwaggerController(DefinitionsManager definitionsManager, ObjectMapper objectMapper, RefResolver refResolver) {
        this.definitionsManager = definitionsManager;
        this.objectMapper = objectMapper;
        this.refResolver = refResolver;
    }

    @RequestMapping(value = "/api/docs/swagger.json", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<Object> getSwaggerDoc() throws IOException {
        ObjectNode doc = (ObjectNode) objectMapper.reader().readTree(new ClassPathResource("/baseSwagger.json").getInputStream());
        ObjectNode paths = objectMapper.createObjectNode();
        ObjectNode definitions = objectMapper.createObjectNode();
        doc.set("paths", paths);
        doc.set("definitions", definitions);
        for (String entityName : definitionsManager.getAllKnownDefinitions()) {
            if (Character.isUpperCase(entityName.charAt(0))) {
                populateEntityActions(paths, entityName);
                populateSubEntityActions(paths, entityName);
                JsonNode schemaDefinition = objectMapper.reader().readTree(definitionsManager.getDefinition(entityName).getContent());
                deleteAll$Ids((ObjectNode) schemaDefinition);
//                definitions.set(entityName, schemaDefinition.get("definitions").get(entityName));
                for (Iterator<String> it = schemaDefinition.get("definitions").fieldNames(); it.hasNext(); ) {
                    String fieldName = it.next();
                    definitions.set(fieldName, schemaDefinition.get("definitions").get(fieldName));
                }
            }
        }
        return new ResponseEntity<>(objectMapper.writeValueAsString(doc), HttpStatus.OK);
    }


    @GetMapping(value = "/api/docs/{file}.json", produces = "application/json")
    public ResponseEntity<Object> getSwaggerDocImportFiles(
            @PathVariable String file
    ) throws IOException {
        JsonNode definitions = refResolver.getResolvedSchema(file, "properties");
        return new ResponseEntity<>(definitions, HttpStatus.OK);
    }


    private void populateEntityActions(ObjectNode paths, String entityName) throws IOException {
        ObjectNode path = objectMapper.createObjectNode();
        PathParameter pathParam = new PathParameter()
                .name("entityId")
                .description(String.format("Id of the %s", entityName))
                .required(true)
                .type("string");
        addGetOperation("entityName", path, Collections.singletonList(pathParam));
        addModifyOperation(entityName, path, Collections.singletonList(pathParam), getBodyParameter(entityName));
        paths.set(String.format("/api/v1/%s/{entityId}", entityName), path);
        path = objectMapper.createObjectNode();
        addPostOperation(entityName, path, Collections.singletonList(getBodyParameter(entityName)));
        paths.set(String.format("/api/v1/%s", entityName), path);
        paths.set(String.format("/api/v1/%s/invite", entityName), path);
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
        addPostOperation("", path, Collections.singletonList(getPropertyCreateRequestBody()));
        paths.set(String.format("/api/v1/%s/{entityId}/{property}", entityName), path);
        path = objectMapper.createObjectNode();
        addPostOperation("", path, Arrays.asList(entityIdParam, propertyParam, propertyIdParam));
        paths.set(String.format("/api/v1/%s/{entityId}/{property}/{propertyId}/send", entityName), path);
    }

    private void addModifyOperation(String entityName, ObjectNode path, List<PathParameter> pathParameters, BodyParameter bodyParameter) throws IOException {
        Operation operation = new Operation()
                .description(String.format("%s new update", entityName));
        pathParameters.forEach(operation::addParameter);
        operation.addParameter(bodyParameter);
        addResponseType(entityName, path, operation, "put");
    }

    private void addPostOperation(String entityName, ObjectNode path, List<Parameter> parameters) throws IOException {
        Operation operation = new Operation()
                .description(String.format("Create new %s", entityName));
        parameters.forEach(operation::addParameter);
        addResponseType(entityName, path, operation, "post");
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

    private void addGetOperation(String entityName, ObjectNode path, List<PathParameter> pathParameters) throws IOException {
        Operation operation = new Operation();
        pathParameters.forEach(operation::addParameter);
        addResponseType(entityName, path, operation, "get");
    }

    private void addResponseType(String entityName, ObjectNode path, Operation operation, String operationType) throws IOException {
        ObjectProperty schema = new ObjectProperty();
        schema.setType("object");
        io.swagger.models.Response response = new io.swagger.models.Response()
                .description("OK")
                .responseSchema(new RefModel(String.format("#/definitions/%s", entityName)));

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
}
