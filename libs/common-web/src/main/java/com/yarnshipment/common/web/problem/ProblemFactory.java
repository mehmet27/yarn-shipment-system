package com.yarnshipment.common.web.problem;

import com.yarnshipment.common.web.request.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * ERR-07 biçiminde {@link ProblemDetail} üretir: {@code type}, {@code title}, {@code status}, {@code detail},
 * {@code instance}, {@code code} ve {@code requestId}. Metinler önce servisin kendi {@code messages} dosyasından,
 * bulunamazsa ortak dosyadan okunur. Servisler kendi hata gövdesini icat etmez (ERR-06).
 */
public final class ProblemFactory {

    private static final String TYPE_PREFIX = "https://errors.yarnshipment.com/";
    private static final Locale TURKISH = Locale.forLanguageTag("tr");

    private final MessageSource serviceMessages;
    private final MessageSource commonMessages;

    public ProblemFactory(MessageSource serviceMessages) {
        this.serviceMessages = serviceMessages;
        ResourceBundleMessageSource common = new ResourceBundleMessageSource();
        common.setBasename("yarnshipment-web-messages");
        common.setDefaultEncoding(StandardCharsets.UTF_8.name());
        this.commonMessages = common;
    }

    public ProblemDetail create(HttpServletRequest request, HttpStatus status, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message(code, "detail"));
        problem.setType(URI.create(TYPE_PREFIX + code.toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setTitle(message(code, "title"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        String requestId = requestId(request);
        if (requestId != null) {
            problem.setProperty("requestId", requestId);
        }
        return problem;
    }

    private String message(String code, String part) {
        String key = "problem." + code + "." + part;
        String text = serviceMessages.getMessage(key, null, null, TURKISH);
        if (text != null) {
            return text;
        }
        String common = commonMessages.getMessage(key, null, key, TURKISH);
        return common != null ? common : key;
    }

    private static @Nullable String requestId(HttpServletRequest request) {
        Object attribute = request.getAttribute(RequestIds.ATTRIBUTE);
        if (attribute instanceof String id) {
            return id;
        }
        String header = request.getHeader(RequestIds.HEADER);
        return header != null ? header : MDC.get(RequestIds.MDC_KEY);
    }
}
