package dev.sunbirdrc.registry.authorization;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SecurityConfigTest {

    @Test
    void securityFilterChain() {
        // Add your test implementation here
    }

    @Test
    void inviteRequestMatcher() {
        SecurityConfig.InviteRequestMatcher matcher = new SecurityConfig.InviteRequestMatcher(
                ".*/invite$", ".*/health", ".*/error", ".*/_schemas/.+$", ".*/templates/.+$",
                ".*/search", "^.+\\.json$", ".*//swagger-ui$", ".*/attestation/.+$",
                ".*/plugin/.+$", ".*/swagger-ui.html$");

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        // Test for /api/v1/Schema
        Mockito.when(request.getRequestURI()).thenReturn("/api/v1/Schema");
        assertFalse(matcher.matches(request), "Pattern should not match /api/v1/Schema");

        Mockito.when(request.getRequestURI()).thenReturn("/api/v1/Teacher%3Fmode=async");
        assertFalse(matcher.matches(request), "Pattern should not match /api/v1/Schema");

        // Add more tests as needed
    }
}