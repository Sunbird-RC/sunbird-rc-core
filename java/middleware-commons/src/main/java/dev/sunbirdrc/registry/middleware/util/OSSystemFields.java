package dev.sunbirdrc.registry.middleware.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections4.ListUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * System fields for created, updated time and userId appended to the json node.
 */
public enum OSSystemFields {

    osCreatedAt {
        @Override
        public void createdAt(JsonNode node, String timeStamp) {
            JSONUtil.addField((ObjectNode) node, osCreatedAt.toString(), timeStamp);
        }
    },
    osUpdatedAt {
        @Override
        public void updatedAt(JsonNode node, String timeStamp) {
            JSONUtil.addField((ObjectNode) node, osUpdatedAt.toString(), timeStamp);
        }

    },
    osCreatedBy {
        @Override
        public void createdBy(JsonNode node, String userId) {
            JSONUtil.addField((ObjectNode) node, osCreatedBy.toString(), userId != null ? userId : "");
        }
    },
    osUpdatedBy {
        @Override
        public void updatedBy(JsonNode node, String userId) {
            JSONUtil.addField((ObjectNode) node, osUpdatedBy.toString(), userId != null ? userId : "");
        }
    },
    osOwner {
      @Override
      public void setOsOwner(JsonNode node, List<String> owners) {
          JSONUtil.addField((ObjectNode) node, osOwner.toString(), ListUtils.emptyIfNull(owners));
      }
    },
    credentials {
        private String getCredentialPropertyName(String signatureProvider) {
            String signatureProperty = _osSignedData.name();
            if(Objects.equals(signatureProvider, "dev.sunbirdrc.registry.service.impl.SignatureV2ServiceImpl")) {
                signatureProperty = _osCredentialId.name();
            }
            return signatureProperty;
        }
        @Override
        public void setCredential(String signatureProvider, JsonNode node, Object signedCredential) {
            if(Objects.equals(signatureProvider, "dev.sunbirdrc.registry.service.impl.SignatureV2ServiceImpl")) {
                ((ObjectNode) node).set(String.valueOf(_osCredentialId), ((ObjectNode) signedCredential).get("id"));
            } else {
                ((ObjectNode) node).set(String.valueOf(_osSignedData), JsonNodeFactory.instance.textNode(signedCredential.toString()));
            }
        }
        @Override
        public void removeCredential(String signatureProvider, JsonNode node) {
            ((ObjectNode) node).put(getCredentialPropertyName(signatureProvider), "");
        }
        @Override
        public JsonNode getCredential(String signatureProvider, JsonNode node) {
            return node.get(getCredentialPropertyName(signatureProvider));
        }
        @Override
        public boolean hasCredential(String signatureProvider, JsonNode node) {
            String property =  getCredentialPropertyName(signatureProvider);
            return node.get(property) != null && !node.get(property).asText().isEmpty();
        }
    },
    attestation {
        private String getCredentialPropertyName(String signatureProvider) {
            String signatureProperty = _osAttestedData.name();
            if(Objects.equals(signatureProvider, "dev.sunbirdrc.registry.service.impl.SignatureV2ServiceImpl")) {
                signatureProperty = _osAttestedData.name();
            }
            return signatureProperty;
        }
        @Override
        public void setCredential(String signatureProvider, JsonNode node, Object signedCredential) {
            if(Objects.equals(signatureProvider, "dev.sunbirdrc.registry.service.impl.SignatureV2ServiceImpl")) {
                ((ObjectNode) node).set(String.valueOf(_osAttestedData), ((ObjectNode) signedCredential).get("id"));
            } else {
                ((ObjectNode) node).set(String.valueOf(_osAttestedData), JsonNodeFactory.instance.textNode(signedCredential.toString()));
            }
        }
        @Override
        public void removeCredential(String signatureProvider, JsonNode node) {
            ((ObjectNode) node).put(getCredentialPropertyName(signatureProvider), "");
        }
        @Override
        public JsonNode getCredential(String signatureProvider, JsonNode node) {
            return node.get(getCredentialPropertyName(signatureProvider));
        }
        @Override
        public boolean hasCredential(String signatureProvider, JsonNode node) {
            String property =  getCredentialPropertyName(signatureProvider);
            return node.get(property) != null && !node.get(property).asText().isEmpty();
        }
    },
    _osState, _osClaimId, _osAttestedData, _osSignedData, _osCredentialId;

    public void createdBy(JsonNode node, String userId){};

    public void updatedBy(JsonNode node, String userId){};

    public void createdAt(JsonNode node, String timeStamp){};

    public void updatedAt(JsonNode node, String timeStamp){};

    public void setOsOwner(JsonNode node, List<String> owner) {};

    public static OSSystemFields getByValue(String value) {
        for (final OSSystemFields element : EnumSet.allOf(OSSystemFields.class)) {
            if (element.toString().equals(value)) {
                return element;
            }
        }
        return null;
    }

    public void setCredential(String signatureProvider, JsonNode node, Object signedCredential){};

    public void removeCredential(String signatureProvider, JsonNode node) {};

    public JsonNode getCredential(String signatureProvider, JsonNode node){ return null; };

    public boolean hasCredential(String signatureProvider, JsonNode node) { return  false; }
}
