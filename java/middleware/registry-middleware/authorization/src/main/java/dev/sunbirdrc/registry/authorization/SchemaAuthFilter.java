package dev.sunbirdrc.registry.authorization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SchemaAuthFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(SchemaAuthFilter.class);
    private static final String INVITE_URL_PATTERN = "/api/v1/([A-Za-z0-9_])+/invite(/)?";

    private final Set<String> anonymousInviteSchemas =  new HashSet<>();
    private final Set<String> anonymousSchemas =  new HashSet<>();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String requestUri = request.getRequestURI();
        try {
            if (requestUri.matches(INVITE_URL_PATTERN) &&
                    anonymousInviteSchemas.stream()
                            .map(d -> String.format("/api/v1/%s/invite(/)?(\\\\?.*)?", d))
                            .anyMatch(requestUri::matches)) {
                servletRequest.getRequestDispatcher(((HttpServletRequest) servletRequest).getServletPath()).forward(servletRequest, servletResponse);
                return;
            } else if (!requestUri.matches(INVITE_URL_PATTERN) && anonymousSchemas.stream()
                    .map(d -> String.format("/api/v1/%s(/.*)?(((\\\\?)|(\\\\%s)).*)?", d, "%3F"))
                    .anyMatch(requestUri::matches)) {
                servletRequest.getRequestDispatcher(((HttpServletRequest) servletRequest).getServletPath()).forward(servletRequest, servletResponse);
                return;
            }
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (Exception e) {
            logger.error("Exception while applying security filters: ", e);
            throw e;
        }
    }

    public void appendAnonymousInviteSchema(String schema) {
        anonymousInviteSchemas.add(schema);
    }

    public void appendAnonymousSchema(String schema) {
        anonymousSchemas.add(schema);
    }


    public void appendAnonymousInviteSchema(List<String> entitiesWithAnonymousInviteRoles) {
        anonymousInviteSchemas.addAll(entitiesWithAnonymousInviteRoles);
    }

    public void appendAnonymousSchema(List<String> entitiesWithAnonymousManageRoles) {
        anonymousSchemas.addAll(entitiesWithAnonymousManageRoles);
    }

    public void removeSchema(String schema) {
        anonymousSchemas.remove(schema);
        anonymousInviteSchemas.remove(schema);
    }
}
