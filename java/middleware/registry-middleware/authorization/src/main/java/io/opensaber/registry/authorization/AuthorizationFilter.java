package io.opensaber.registry.authorization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import io.jsonwebtoken.Jwts;
import io.opensaber.registry.authorization.pojos.AuthInfo;
import io.opensaber.registry.middleware.BaseMiddleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;

public class AuthorizationFilter implements BaseMiddleware{
	
	private static Logger logger = LoggerFactory.getLogger(AuthorizationFilter.class);
	
	private static final String TOKEN_IS_MISSING = "Auth token is missing";
	private static final String TOKEN_IS_INVALID = "Auth token is invalid";
	
	public Map<String,Object> execute(Map<String,Object> mapObject) throws MiddlewareHaltException{
		Object tokenObject = mapObject.get(Constants.TOKEN_OBJECT);
		if(tokenObject==null){
			throw new MiddlewareHaltException(TOKEN_IS_MISSING);
		}
		String token = tokenObject.toString();
		AuthInfo authInfo = extractTokenIntoAuthInfo(token);
		if(authInfo.getSub() == null || authInfo.getAud() == null || authInfo.getName() == null){
			throw new MiddlewareHaltException(TOKEN_IS_INVALID);
		}
		List<SimpleGrantedAuthority> authorityList = new ArrayList<SimpleGrantedAuthority>();
		authorityList.add(new SimpleGrantedAuthority(authInfo.getAud()));
		AuthorizationToken  authorizationToken = new AuthorizationToken(authInfo, authorityList);
		SecurityContextHolder.getContext().setAuthentication(authorizationToken);
		return mapObject;
	}
	
	public AuthInfo extractTokenIntoAuthInfo(String token){
		AuthInfo authInfo = new AuthInfo();
		
		try {
		    Jwts.parser()
		        .setSigningKeyResolver(authInfo)
		        .parseClaimsJws(token);
		} catch (Exception e) {
			logger.info("Claim extracted but verification failed");
		}
		return authInfo;
	}

	/* (non-Javadoc)
	 * @see io.opensaber.registry.middleware.BaseMiddleware#next(java.util.Map)
	 */
	public Map<String, Object> next(Map<String, Object> mapData) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	

}
