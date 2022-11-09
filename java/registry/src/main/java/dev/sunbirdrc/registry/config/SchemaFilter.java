package dev.sunbirdrc.registry.config;

import dev.sunbirdrc.plugin.components.SpringContext;
import dev.sunbirdrc.registry.util.DefinitionsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

@Component
public class SchemaFilter implements Filter {

    private static final String INVITE_URL_ENDPOINT = "/invite";
    private static final String SEND_URL_ENDPOINT = "/send";
    private static final String URL_ENDPOINT = "/api/v1/";
    @Autowired
    private DefinitionsManager definitionsManager;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String entityName;
        List<String> rolesArr;
        try {
            if(request.getMethod().equals(HttpMethod.POST.toString()) && !request.getRequestURI().contains(SEND_URL_ENDPOINT)) {
                if (request.getRequestURI().contains(INVITE_URL_ENDPOINT)) {
                    entityName = request.getRequestURI().substring(request.getRequestURI().indexOf(URL_ENDPOINT) + URL_ENDPOINT.length(), request.getRequestURI().indexOf(INVITE_URL_ENDPOINT));
                    rolesArr = definitionsManager.getDefinition(entityName).getOsSchemaConfiguration().getInviteRoles();
                } else {
                    entityName = request.getRequestURI().substring(request.getRequestURI().indexOf(URL_ENDPOINT) + URL_ENDPOINT.length());
                    if(entityName.contains("/") && !entityName.endsWith("/")) {
                        filterChain.doFilter(servletRequest, servletResponse);
                        return;
                    }
                    if(entityName.endsWith("/")) entityName = entityName.substring(0, entityName.length() - 1);
                    rolesArr = definitionsManager.getDefinition(entityName).getOsSchemaConfiguration().getRoles();
                }
                if (dispatchRequestIfRoleContainsAnonymous(servletRequest, servletResponse, rolesArr)) return;
            }
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean dispatchRequestIfRoleContainsAnonymous(ServletRequest servletRequest, ServletResponse servletResponse, List<String> rolesArr) throws ServletException, IOException {
        if (rolesArr.contains("anonymous")) {
            servletRequest.getRequestDispatcher(((HttpServletRequest) servletRequest).getServletPath()).forward(servletRequest, servletResponse);
            return true;
        }
        return false;
    }
}
