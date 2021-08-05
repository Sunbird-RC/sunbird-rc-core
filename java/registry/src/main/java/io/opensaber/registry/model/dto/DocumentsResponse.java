package io.opensaber.registry.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DocumentsResponse {
    List<String> documentLocations = new ArrayList<>();
    List<String> errors = new ArrayList<>();

    public void addDocumentLocation(String objectName) {
        documentLocations.add(objectName);
    }

    public void addError(String fileName) {
        errors.add(fileName);
    }
}
