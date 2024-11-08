// codeql [java/unvalidated-url-forward]: accept reason="This is a known and accepted risk for this specific file."
// False positive because, this code checks if the requestUri matches a specific pattern and if any of the anonymousInviteSchemas match a dynamically generated pattern.
// If both conditions are true, it forwards the request to the same path.
package dev.sunbirdrc.registry.authorization;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
@SuppressWarnings("java/URL-forward-from-remote-source")
public class SchemaAuthFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(SchemaAuthFilter.class);
    private static final String INVITE_URL_PATTERN = "/api/v1/([A-Za-z0-9_])+/invite(/)?";

    private final Set<String> anonymousInviteSchemas = new HashSet<>();
    private final Set<String> anonymousSchemas = new HashSet<>();

    @Override
    protected void doFilterInternal(HttpServletRequest servletRequest, HttpServletResponse servletResponse, FilterChain filterChain) throws ServletException, IOException {

        String requestUri = servletRequest.getRequestURI();

        try {
            if (requestUri.matches(INVITE_URL_PATTERN) &&
                    anonymousInviteSchemas.stream()
                            .map(d -> String.format("/api/v1/%s/invite(/)?(\\?.*)?", d))
                            .anyMatch(requestUri::matches)) {
                servletRequest.getRequestDispatcher(servletRequest.getServletPath()).forward(servletRequest, servletResponse);
                return;
            } else if (!requestUri.matches(INVITE_URL_PATTERN) &&
                    anonymousSchemas.stream()
                            .map(d -> String.format("/api/v1/%s([^/]+)?(((\\?)|(\\%s)).*)?", d, "%3F"))
                            .anyMatch(requestUri::matches)) {
                logger.debug("Forwarded NON Invite and Anonymous to : {} anonymousSchemas {} ", servletRequest.getServletPath(), anonymousSchemas);
                servletRequest.getRequestDispatcher(servletRequest.getServletPath()).forward(servletRequest, servletResponse);
                return;
            }
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (Exception e) {
            logger.error("Exception while applying security filters: ", e);
            throw e;
        } finally {
            logger.debug("Exiting SchemaAuthFilter for URI: {} ", requestUri);
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
