package dev.sunbirdrc.registry.model.attestation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;

public class AttestationPath {
    private static final String ARRAY_STEP = "[]" ;
    private static final String SEP = "/";
    private final String path;
    private transient List<String> steps;

    public AttestationPath(String path) {
        this.path = path;
    }

    private void setSteps() {
        if (this.steps != null) {
            return;
        }

        List<String> steps = new ArrayList<>();
        StringBuilder curr = new StringBuilder();
        for (String step: path.split(SEP)) {
            if (!step.equals(ARRAY_STEP)) {
                curr.append(SEP).append(step);
            } else {
                if (curr.length() > 0) steps.add(curr.toString());
                steps.add(step);
                curr.setLength(0);
            }
        }
        if (curr.length() > 0) steps.add(curr.toString());
        this.steps = Collections.unmodifiableList(steps);
    }

    public String getPath() {
        return this.path;
    }

    public Set<EntityPropertyURI> getEntityPropertyURIs(JsonNode node, String uuidPropertyName) {
        Queue<EntityPropertyURI> currPaths = new LinkedList<EntityPropertyURI>(){{
            add(new EntityPropertyURI("", ""));
        }};
        if (steps == null) setSteps();
        for (String step: steps) {
            int currCount = currPaths.size();
            for (int i = 0; i < currCount; i++) {
                EntityPropertyURI currPath = currPaths.remove();
                if (!step.equals(ARRAY_STEP)) {
                    currPaths.add(EntityPropertyURI.merge(currPath, step, step));
                    continue;
                }
                JsonNode currNode = node.at(currPath.getJsonPointer());
                if (currNode == null || currNode.isMissingNode()) {
                    continue;
                }
                ArrayNode arrayNode = null;
                ObjectNode objectNode = null;
                if(currNode instanceof ArrayNode){
                    arrayNode = (ArrayNode) currNode;
                    for (int j = 0; j < arrayNode.size(); j++) {
                        if (arrayNode.get(j).isObject()) {
                            JsonNode uuidNode = arrayNode.get(j).get(uuidPropertyName);
                            String uuidPointer = SEP;
                            if (uuidNode == null || uuidNode.isMissingNode()) {
                                uuidPointer = uuidPointer + EntityPropertyURI.NO_UUID;
                            } else {
                                uuidPointer = uuidPointer + uuidNode.asText();
                            }
                            currPaths.add(EntityPropertyURI.merge(currPath,
                                    uuidPointer,
                                    SEP + j
                            ));
                        } else {
                            currPaths.add(EntityPropertyURI.merge(currPath, SEP + j, SEP + j));
                        }
                    }
                } else if(currNode instanceof ObjectNode){
                    objectNode = (ObjectNode) currNode;
                    String uuidPointer = SEP;
                    if (objectNode == null || objectNode.isMissingNode()) {
                        uuidPointer = uuidPointer + EntityPropertyURI.NO_UUID;
                    } else {
                        uuidPointer = uuidPointer + objectNode.asText();
                    }
                    currPaths.add(EntityPropertyURI.merge(currPath,
                            uuidPointer,
                            SEP
                    ));
                }


            }
        }
        return new HashSet<>(currPaths);
    }
}
