package dev.sunbirdrc.registry.config;

import dev.sunbirdrc.registry.util.IDefinitionsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class SchemaFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(SchemaFilter.class);
    private static final String INVITE_URL_ENDPOINT = "/invite";
    @Autowired
    private IDefinitionsManager definitionsManager;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        try {
            if (request.getRequestURI().contains(INVITE_URL_ENDPOINT) &&
                    definitionsManager.getEntitiesWithAnonymousInviteRoles().stream().anyMatch(request.getRequestURI()::contains)) {
                servletRequest.getRequestDispatcher(((HttpServletRequest) servletRequest).getServletPath()).forward(servletRequest, servletResponse);
                return;
            } else if (definitionsManager.getEntitiesWithAnonymousManageRoles().stream().anyMatch(request.getRequestURI()::contains)) {
                servletRequest.getRequestDispatcher(((HttpServletRequest) servletRequest).getServletPath()).forward(servletRequest, servletResponse);
                return;
            }
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (Exception e) {
            logger.error("Exception while applying security filters: ", e);
            throw e;
        }
    }
}
