package dev.sunbirdrc.registry.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import dev.sunbirdrc.registry.entities.UserToken;
import dev.sunbirdrc.registry.model.OAuth2Properties;
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
	private static final Logger logger = LoggerFactory.getLogger(CustomJwtAuthenticationConverter.class);

	private final OAuth2Properties oAuth2Properties;

	public CustomJwtAuthenticationConverter(OAuth2Properties oAuth2Properties) {
		this.oAuth2Properties = oAuth2Properties;
	}

	@Override
	public AbstractAuthenticationToken convert(Jwt source) {
		try {
			DocumentContext documentContext = JsonPath.parse(source.getClaims());
			List<String> roles = getValue(documentContext, oAuth2Properties.getRoles(), ArrayList.class);
			String email = getValue(documentContext, oAuth2Properties.getEmail(), String.class);
			Map<String, Integer> consentFields = getValue(documentContext, oAuth2Properties.getConsent(), Map.class);
			List<String> entities = getValue(documentContext, oAuth2Properties.getEntity(), ArrayList.class);
			return new UserToken(source, email, consentFields, entities,
					roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));
		} catch (Exception e) {
			return new UserToken(source, "", Collections.emptyMap(), Collections.emptyList(), Collections.emptyList());
		}

	}

	private  <T> T getValue(DocumentContext documentContext, String path, Class<T> type) {
		try {
			return documentContext.read(path, type);
		} catch (Exception e) {
			logger.debug("Fetching {} from token claims failed", path, e);
		}
		return getDefaultValue(type);
	}

	private <T> T getDefaultValue(Class<T> type) {
		if (String.class.equals(type)) {
			return (T) "";
		} else if (Map.class.equals(type)) {
			return (T) Collections.emptyMap();
		} else if (ArrayList.class.equals(type)) {
			return (T) Collections.emptyList();
		}
		return null;
	}

}
