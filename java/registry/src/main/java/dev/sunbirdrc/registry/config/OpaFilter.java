package dev.sunbirdrc.registry.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.pojos.OPADataRequest;
import dev.sunbirdrc.pojos.OPADataResponse;
import dev.sunbirdrc.pojos.Response;
import dev.sunbirdrc.pojos.ResponseParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.OutputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
@Component
@Configuration
public class OpaFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(OpaFilter.class);

    @Value("${opa.enabled}") private boolean enabled;
    @Value("${opa.url}") private String url;
    @Value("${opa.path}") private String opaPackagePath;
    @Value("${opa.allowKey}") private String opaAllowKey;

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        Map<String, String> headers = new HashMap<>();
        for (Enumeration<String> headerNames = request.getHeaderNames(); headerNames.hasMoreElements();) {
            String header = headerNames.nextElement();
            headers.put(header, request.getHeader(header));
        }
        String[] path = request.getRequestURI().replaceAll("^/|/$", "").split("/");
        Map<String, Object> input = new HashMap<>();
        input.put("method", request.getMethod());
        input.put("path", path);
        input.put("headers", headers);
        try {
            input.put("principal", request.getUserPrincipal().getName());
        } catch (Exception ignored) {}
        try {
            input.put("token", request.getHeader("authorization").split(" ")[1]);
        } catch (Exception ignored) {}

        RestTemplate client = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();
        HttpEntity<?> request1 = new HttpEntity<>(new OPADataRequest(input));
        OPADataResponse opaDataResponse = client.postForObject(String.format("%s%s", url, opaPackagePath), request1, OPADataResponse.class);
        boolean isAllowed = opaDataResponse == null || opaDataResponse.getResult() == null || !((boolean) opaDataResponse.getResult().getOrDefault(opaAllowKey, false));
        logger.info("OPA Request check is completed {}", opaDataResponse);
        if(isAllowed) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            ResponseParams responseParams = new ResponseParams();
            Response response2 = new Response(Response.API_ID.NONE, HttpStatus.UNAUTHORIZED.name(), responseParams);
            responseParams.setErrmsg("Policy doesn't allow the request");
            if(opaDataResponse != null) responseParams.setResultList(Collections.singletonList(opaDataResponse.getResult()));
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            OutputStream responseStream = response.getOutputStream();
            mapper.writeValue(responseStream, response2);
            responseStream.flush();
            return;
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return !enabled;
    }
}
