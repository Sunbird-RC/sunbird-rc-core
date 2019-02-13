package io.opensaber.registry.controller;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.registry.authorization.AuthorizationToken;
import io.opensaber.registry.authorization.pojos.AuthInfo;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.sink.DBProviderFactory;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.tests.utility.TestHelper;
import org.apache.commons.lang.StringUtils;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.UUID;

@SpringBootTest
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
abstract public class RegistryTestBase {

	public static final String FORMAT = "JSON-LD";
	private static final String INVALID_SUBJECT_LABEL = "ex:Picasso";
	private static final String REPLACING_SUBJECT_LABEL = "!samp131d";
	private static final String VALID_JSONLD = "school.jsonld";
	private static final String CONTEXT_CONSTANT = "sample:";
	public String jsonld;
	@Value("${registry.context.base}")
	private String registryContextBase;

	public DatabaseProvider databaseProvider;
	@Autowired
	public DBProviderFactory dbProviderFactory;

	public RegistryTestBase() {
		databaseProvider = dbProviderFactory.getInstance(null);
		MockitoAnnotations.initMocks(this);
		TestHelper.clearData(databaseProvider);
		databaseProvider.getGraphStore().addVertex(Constants.GRAPH_GLOBAL_CONFIG).property(Constants.PERSISTENT_GRAPH,
				true);
		AuthInfo authInfo = new AuthInfo();
		authInfo.setAud("aud");
		authInfo.setName("name");
		authInfo.setSub("sub");
		AuthorizationToken authorizationToken = new AuthorizationToken(authInfo,
				Collections.singletonList(new SimpleGrantedAuthority("blah")));
		SecurityContextHolder.getContext().setAuthentication(authorizationToken);
	}

	public static String generateRandomId() {
		return UUID.randomUUID().toString();
	}

	public void setJsonld(String filename) {

		try {
			String file = Paths.get(getPath(filename)).toString();
			jsonld = readFromFile(file);
		} catch (Exception e) {
			jsonld = StringUtils.EMPTY;
		}

	}

	public UUID getLabel() {
		UUID label = UUID.randomUUID();
		return label;
	}

	public String readFromFile(String file) throws IOException, FileNotFoundException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		StringBuilder sb = new StringBuilder();
		try {
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
		} catch (Exception e) {
			return StringUtils.EMPTY;
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
		return sb.toString();
	}

	public URI getPath(String file) throws URISyntaxException {
		return this.getClass().getClassLoader().getResource(file).toURI();
	}

	public String generateBaseUrl() {
		return Constants.INTEGRATION_TEST_BASE_URL;
	}

	public String getValidJsonString(String fileName) {
		setJsonld(fileName);
		return jsonld;
	}

	public void setJsonldWithNewRootLabel(String label) {
		jsonld = jsonld.replace(REPLACING_SUBJECT_LABEL, label);
	}

	public void setJsonldWithNewRootLabel() {
		while (jsonld.contains(REPLACING_SUBJECT_LABEL)) {
			jsonld = jsonld.replaceFirst(REPLACING_SUBJECT_LABEL, CONTEXT_CONSTANT + generateRandomId());
		}
	}

	public String getValidStringForUpdate(String reqId){
		ObjectNode childnode = JsonNodeFactory.instance.objectNode();
		ObjectNode parentNode = JsonNodeFactory.instance.objectNode();
		childnode.put("id",reqId);
		childnode.put("gender","GenderTypeCode-FEMALE");
		parentNode.set("Teacher",childnode);
		return parentNode.toString();
	}

}
