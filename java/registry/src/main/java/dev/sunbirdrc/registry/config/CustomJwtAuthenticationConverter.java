package dev.sunbirdrc.registry.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import dev.sunbirdrc.registry.entities.UserToken;
import dev.sunbirdrc.registry.util.Definition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;

class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
	private static final String ENTITY = "entity";
	private static Logger logger = LoggerFactory.getLogger(CustomJwtAuthenticationConverter.class);

	private static final String ROLES = "roles";
	private static final String EMAIL = "email";
	private static final String CONSENT = "consent";
	JsonNode issuerNode;

	public CustomJwtAuthenticationConverter(JsonNode issuerNode) {
		this.issuerNode = issuerNode;
	}

	@Override
	public AbstractAuthenticationToken convert(Jwt source) {
		try {
			DocumentContext documentContext = JsonPath.parse(source.getClaims());
			List<String> roles = new ArrayList<>();
			try {
				roles = documentContext.read(issuerNode.get(ROLES).asText("realm_access.roles"), ArrayList.class);
			} catch (Exception e) {
				logger.error("Fetching roles from token claims failed", e);
			}
			String email = "";
			try {
				email = documentContext.read(issuerNode.get(EMAIL).asText(EMAIL), String.class);
			} catch (Exception e) {
				logger.error("Fetching emaild from token claims failed", e);
			}
			Map consentFields = new HashMap();
			try {
				consentFields = documentContext.read(issuerNode.get(CONSENT).asText(CONSENT), Map.class);
			} catch (Exception e) {
				logger.error("Fetching consentFields from token claims failed", e);
			}
			List<String> entities = new ArrayList<>();
			try {
				entities = documentContext.read(issuerNode.get(ENTITY).asText(ENTITY), ArrayList.class);
			} catch (Exception e) {
				logger.error("Fetching entities from token claims failed", e);
			}
			return new UserToken(source, email, consentFields, entities,
					roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));
		} catch (Exception e) {
			return new UserToken(source, "", Collections.emptyMap(), Collections.emptyList(), Collections.emptyList());
		}

	}

}
