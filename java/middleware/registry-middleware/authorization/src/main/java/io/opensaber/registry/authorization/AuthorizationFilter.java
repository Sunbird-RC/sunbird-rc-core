package io.opensaber.registry.authorization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.opensaber.pojos.OpenSaberInstrumentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import io.jsonwebtoken.Jwts;
import io.opensaber.registry.authorization.pojos.AuthInfo;
import io.opensaber.registry.middleware.Middleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;
import org.apache.commons.codec.binary.Base64;

public class AuthorizationFilter implements Middleware {

    private static Logger logger = LoggerFactory.getLogger(AuthorizationFilter.class);
    private static final String TOKEN_IS_MISSING = "Auth token is missing";
    private static final String VERIFICATION_EXCEPTION = "Auth token is invalid";

    private KeyCloakServiceImpl keyCloakServiceImpl;

    public AuthorizationFilter() {}

    public AuthorizationFilter(KeyCloakServiceImpl keyCloakServiceImpl) {
        this.keyCloakServiceImpl = keyCloakServiceImpl;
    }

    @Autowired
    private OpenSaberInstrumentation watch;

    /**
     * This method validates JWT access token against Sunbird Keycloak server and sets the valid access token to a map object
     * @param mapObject
     * @throws MiddlewareHaltException
     */

      public Map<String, Object> execute(Map<String, Object> mapObject) throws MiddlewareHaltException {
          Object tokenObject = mapObject.get(Constants.TOKEN_OBJECT);

          if (tokenObject == null || tokenObject.toString().trim().isEmpty()) {
              throw new MiddlewareHaltException(TOKEN_IS_MISSING);
          }
          String token = tokenObject.toString();
          try {
              watch.start("KeycloakServiceImpl.verifyToken");
              String userId = keyCloakServiceImpl.verifyToken(token);
              watch.stop("KeycloakServiceImpl.verifyToken");

              if (!userId.trim().isEmpty()) {

                  if (mapObject.containsKey("userName")) {
                      logger.debug("Access token for user {} verified successfully with KeyCloak server !", mapObject.get("userName"));
                  } else {
                      logger.debug("Access token verified successfully with KeyCloak server !");
                  }
                  AuthInfo authInfo = extractTokenIntoAuthInfo(token);
                  if (authInfo.getSub() == null || authInfo.getAud() == null || authInfo.getName() == null) {
                      throw new MiddlewareHaltException(VERIFICATION_EXCEPTION);
                  }
                  List<SimpleGrantedAuthority> authorityList = new ArrayList<>();

                  authorityList.add(new SimpleGrantedAuthority(authInfo.getAud()));
                  AuthorizationToken authorizationToken = new AuthorizationToken(authInfo, authorityList);
                  SecurityContextHolder.getContext().setAuthentication(authorizationToken);
              } else {
                  throw new MiddlewareHaltException(VERIFICATION_EXCEPTION);
              }
          } catch (Exception e) {
              logger.error("AuthorizationFilter: MiddlewareHaltException !");
              throw new MiddlewareHaltException(VERIFICATION_EXCEPTION);
          }
          return mapObject;
          }

    /**
     * This method extracts Authorisation information ,i.e. AuthInfo from input JWT access token
     *
     * @param token
     */
    public AuthInfo extractTokenIntoAuthInfo(String token) {
        AuthInfo authInfo = new AuthInfo();
        try {
            Jwts.parser().setSigningKey(keyCloakServiceImpl.getPublicKey()).parseClaimsJws(token);

            String[] split_string = token.split("\\.");
            String base64EncodedBody = split_string[1];
            Base64 base64Url = new Base64(true);
            String body = new String(base64Url.decode(base64EncodedBody));

            Map<String, Object> map = new Gson().fromJson(
                    body, new TypeToken<HashMap<String, Object>>() {}.getType()
            );

            for (String s : map.keySet()) {
                if (s.equalsIgnoreCase("aud")) {
                    authInfo.setAud(map.get(s).toString());
                }
                if (s.equalsIgnoreCase("sub")) {
                    authInfo.setSub(map.get(s).toString());
                }
                if (s.equalsIgnoreCase("name")) {
                    authInfo.setName(map.get(s).toString());
                }
            }
        } catch (Exception e) {
            logger.error("Claim extracted but verification failed !", e);
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
