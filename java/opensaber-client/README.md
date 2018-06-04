## Opensaber client library

A thin wrapper client for Opensaber registry APIs.

#### Environment variables to override default configuration

* opensaber_client_environment - Specifies the client environment. Defaults to dev.
* registry_service_baseurl - A http url which points to a registry installtion
* registry_client_connect_timeout - Connection timeout in ms for the underlying http client
* registry_client_read_timeout - Socket read timeout in ms for the underlying http client
* registry_client_connection_request_timeout - Http connection request timeout from underlying connection manager

#### Build from source

The following commands generates a jar artifact of the Opensaber client library

```sh
git clone https://github.com/project-sunbird/open-saber.git
cd open-saber/java/opensaber-client
mvn clean install
```

#### Invoking the client APIs

The client library API definitions can be found [here](https://github.com/project-sunbird/open-saber/blob/master/java/opensaber-client/src/main/java/io/opensaber/registry/client/Client.java). A sample java code to invoke one of the client APIs is specified below:

```java

import io.opensaber.registry.client.Client;
import io.opensaber.registry.client.OpensaberClient;
import io.opensaber.registry.client.data.RequestData;
import io.opensaber.registry.client.data.ResponseData;
import io.opensaber.registry.transform.ITransformer;
import io.opensaber.registry.transform.JsonToJsonLDTransformer;
import io.opensaber.registry.transform.JsonldToJsonTransformer;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

// Transforms the input JSON data to JSON-LD format
ITransformer<String> jsonToJsonldTransformer = JsonToJsonLDTransformer.getInstance();
// Transforms the output JSON-LD data to JSON format
ITransformer<String> jsonldToJsonTransformer = JsonldToJsonTransformer.getInstance();
Client<String, String> client =
    OpensaberClient.builder()
    .requestTransformer(jsonToJsonldTransformer)
    .responseTransformer(jsonldToJsonTransformer).build();

Map<String, String> headers = new HashMap<>();
headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);
headers.put("x-authenticated-user-token", "JWTAuthenticationToken");

try {
    ResponseData<String> responseData = client.addEntity(new RequestData<>("JSON Input String"), headers);
} catch (TransformationException ex) {
    ex.printStackTrace();
}

```