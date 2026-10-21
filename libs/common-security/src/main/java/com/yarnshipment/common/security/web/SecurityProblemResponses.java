package com.yarnshipment.common.security.web;

import com.yarnshipment.common.security.scope.OutOfScopeAccessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

/**
 * Filtre zincirinde oluşan 401 ve 403'leri ERR-07 biçiminde {@code application/problem+json} olarak yazar. Controller
 * içindeki hatalar {@code common-web} advice'ına gider; buradaki gövde onunla aynı alanları taşır.
 */
public final class SecurityProblemResponses implements AuthenticationEntryPoint, AccessDeniedHandler {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    private static final String TYPE_PREFIX = "https://errors.yarnshipment.com/";
    private static final Locale TURKISH = Locale.forLanguageTag("tr");

    private final MessageSource messages;
    private final JsonMapper jsonMapper;

    public SecurityProblemResponses(MessageSource messages, JsonMapper jsonMapper) {
        this.messages = Objects.requireNonNull(messages, "messages");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(request, response, SecurityErrorCode.AUTHENTICATION_REQUIRED);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException {
        SecurityErrorCode code = exception instanceof OutOfScopeAccessException
                ? SecurityErrorCode.OUT_OF_SCOPE
                : SecurityErrorCode.ACCESS_DENIED;
        write(request, response, code);
    }

    private void write(HttpServletRequest request, HttpServletResponse response, SecurityErrorCode code)
            throws IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), body(request, code));
    }

    private Map<String, Object> body(HttpServletRequest request, SecurityErrorCode code) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", TYPE_PREFIX + code.slug());
        body.put("title", message(code, "title"));
        body.put("status", code.status().value());
        body.put("detail", message(code, "detail"));
        body.put("instance", request.getRequestURI());
        body.put("code", code.name());
        String requestId = requestId(request);
        if (requestId != null) {
            body.put("requestId", requestId);
        }
        return body;
    }

    private String message(SecurityErrorCode code, String part) {
        return messages.getMessage("problem." + code.name() + "." + part, null, TURKISH);
    }

    private static @Nullable String requestId(HttpServletRequest request) {
        String header = request.getHeader(REQUEST_ID_HEADER);
        return header == null ? MDC.get(REQUEST_ID_MDC_KEY) : header;
    }
}
