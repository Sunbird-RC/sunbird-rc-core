package io.opensaber.validators.shex.shaclex;

import java.util.Optional;

public class ShaclexErrorData {

    private String nodeValueWithError;
    private String readableErrorMessage;
    private Optional<String> nodeDataType;
    private Optional<String> iriNode;

    public ShaclexErrorData(String nodeValueWithError, String readableErrorMessage,
                            Optional<String> nodeDataType, Optional<String> iriNode) {
        this.nodeValueWithError = nodeValueWithError;
        this.readableErrorMessage = readableErrorMessage;
        this.nodeDataType = nodeDataType;
        this.iriNode = iriNode;
    }

    public String getReadableErrorMessage() {
        return readableErrorMessage;
    }

    public String getNodeValueWithError() {
        return nodeValueWithError;
    }

    public Optional<String> getNodeDataType() {
        return nodeDataType;
    }

    public Optional<String> getIriNode() {
        return iriNode;
    }
}
