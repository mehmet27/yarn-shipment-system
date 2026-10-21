package com.yarnshipment.common.security.web;

import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Servisler arası {@code RestClient} çağrısına gelen isteğin token'ını ve {@code X-Request-Id}'sini ekler (mimari
 * doküman, token relay). Çağrılan servis token'ı yeniden doğrular (SEC-01); aktör kimliği gövdede taşınmaz (SEC-04).
 */
public final class TokenRelayInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        HttpHeaders headers = request.getHeaders();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token
                && !headers.containsHeader(HttpHeaders.AUTHORIZATION)) {
            headers.setBearerAuth(token.getToken().getTokenValue());
        }
        String requestId = MDC.get(SecurityProblemResponses.REQUEST_ID_MDC_KEY);
        if (requestId != null && !headers.containsHeader(SecurityProblemResponses.REQUEST_ID_HEADER)) {
            headers.set(SecurityProblemResponses.REQUEST_ID_HEADER, requestId);
        }
        return execution.execute(request, body);
    }
}
