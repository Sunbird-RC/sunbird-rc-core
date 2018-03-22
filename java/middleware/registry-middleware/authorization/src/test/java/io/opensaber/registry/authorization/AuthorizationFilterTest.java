package io.opensaber.registry.authorization;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.security.core.context.SecurityContextHolder;

import io.opensaber.registry.authorization.pojos.AuthInfo;
import io.opensaber.registry.middleware.BaseMiddleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;

public class AuthorizationFilterTest {
	
	private BaseMiddleware baseM;
	
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();
	
	@Before
	public void initialize(){
		baseM = new AuthorizationFilter();
	}

	@Test
	public void test_missing_auth_token() throws MiddlewareHaltException, IOException{
		expectedEx.expectMessage("Auth token is missing");
		expectedEx.expect(MiddlewareHaltException.class);
		Map<String,Object> mapObject = new HashMap<String,Object>();
		baseM.execute(mapObject);
	}
	
	
	@Test
	public void test_valid_token() throws MiddlewareHaltException, IOException{
		Map<String,Object> mapObject = new HashMap<String,Object>();
		mapObject.put(Constants.TOKEN_OBJECT, "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ1WXhXdE4tZzRfMld5MG5P"
				+ "S1ZoaE5hU0gtM2lSSjdXU25ibFlwVVU0TFRrIn0.eyJqdGkiOiI2OTBiNDZjZS03MjI5LTQ5NjgtODU4Yy0yMzNjNmJhZjMxODMiLCJleHAiOjE1MjE1NjI0NDUsIm5iZiI6MCwiaWF0IjoxNTIxNTE5MjQ1LCJpc3MiOiJodHRwczovL3N0YWdpbmcub3Blbi1zdW5iaXJkLm9yZy9hdXRoL3"
				+ "JlYWxtcy9zdW5iaXJkIiwiYXVkIjoiYWRtaW4tY2xpIiwic3ViIjoiYWJkYmRjYzEtZDI5Yy00ZTQyLWI1M2EtODVjYTY4NzI3MjRiIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiYWRtaW4tY2xpIiwiYXV0aF90aW1lIjowLCJzZXNzaW9uX3N0YXRlIjoiZmZiYWE2ZWUtMDhmZi00OGVlLThlYTEt"
				+ "ZTI3YzhlZTE5ZDVjIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6W10sInJlc291cmNlX2FjY2VzcyI6e30sIm5hbWUiOiJSYXl1bHUgVmlsbGEiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ2cmF5dWx1IiwiZ2l2ZW5fbmFtZSI6IlJheXVsdSIsImZhbWlseV9uYW1lIjoiVmlsbGEiLCJlbWF"
				+ "pbCI6InJheXVsdUBnbWFpbC5jb20ifQ.U1hsUoXGYKtYssOkytMo_tnexHhwKs86IXrDw8rhL9tpG5c6DArVJvdhn5wTEbgzp52efNwQ5LrGGmpBFRWDw0szA5ggT347RCbTTxXZEFF2bUEE8rr0KbkfPOwk5Gazo_xRerW-URyWPlzqppZaUPc6kzY8TDouGmKF8qyVenaxrRgbhKNRYbZWFviARLyt"
				+ "ZTMLtgLafhmOvj6r3vK-kt36afUNROBSoNaxhcvSF9QnTRB1_0Bnb_qyVMqEDSdwZdGs3rMU_W8SFWMewxxXPuYWEXIvXIr2AMs7naCR4colLGz8AOMFR44-qTEF-eF71qqBNouh1hgd4N0l4sKzxA");
		baseM.execute(mapObject);
		AuthInfo authInfo = (AuthInfo)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		assertNotNull(authInfo.getSub());
		assertNotNull(authInfo.getAud());
	}
	
	@Test
	public void test_invalid_token() throws MiddlewareHaltException, IOException{
		expectedEx.expectMessage("Auth token is invalid");
		expectedEx.expect(MiddlewareHaltException.class);
		Map<String,Object> mapObject = new HashMap<String,Object>();
		mapObject.put(Constants.TOKEN_OBJECT, "invalid.token.");
		baseM.execute(mapObject);
	}

}
