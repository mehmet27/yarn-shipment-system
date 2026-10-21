package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.common.security.web.SecurityProblemResponses;
import com.yarnshipment.shipment.application.port.in.ShipmentNotFoundException;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Servise özgü hataların ERR-07 cevabı. {@code common-web} gelince ortak advice'a taşınır (ERR-06); güvenlik
 * hataları (401/403) {@code common-security} tarafından yazılır.
 */
/* Spring Boot'un genel ProblemDetail işleyicisinden önce çalışır; aksi halde eksik başlık code alanı olmadan döner. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public final class ShipmentExceptionHandler {

    private static final String TYPE_PREFIX = "https://errors.yarnshipment.com/";
    private static final Locale TURKISH = Locale.forLanguageTag("tr");

    private final MessageSource messages;

    public ShipmentExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(ShipmentNotFoundException.class)
    public ProblemDetail handleNotFound(HttpServletRequest request) {
        return problem(request, HttpStatus.NOT_FOUND, "SHIPMENT_NOT_FOUND");
    }

    @ExceptionHandler(InvalidShipmentStatusException.class)
    public ProblemDetail handleInvalidStatus(HttpServletRequest request) {
        return problem(request, HttpStatus.CONFLICT, "INVALID_SHIPMENT_STATUS");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ProblemDetail handleMissingHeader(MissingRequestHeaderException exception, HttpServletRequest request) {
        ProblemDetail problem = problem(request, HttpStatus.BAD_REQUEST, "MISSING_HEADER");
        problem.setProperty("errors", List.of(Map.of("field", exception.getHeaderName(), "message", "required")));
        return problem;
    }

    /** Sözleşmedeki sınırlar (ör. {@code size <= 100}) {@code @Validated} arayüzde denetlenir (API-07). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
        ProblemDetail problem = problem(request, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
        List<Map<String, String>> errors = exception.getConstraintViolations().stream()
                .map(violation -> Map.of(
                        "field", lastNode(violation.getPropertyPath().toString()), "message", violation.getMessage()))
                .toList();
        problem.setProperty("errors", errors);
        return problem;
    }

    private static String lastNode(String path) {
        return path.substring(path.lastIndexOf('.') + 1);
    }

    private ProblemDetail problem(HttpServletRequest request, HttpStatus status, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message(code, "detail"));
        problem.setType(URI.create(TYPE_PREFIX + code.toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setTitle(message(code, "title"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        String requestId = request.getHeader(SecurityProblemResponses.REQUEST_ID_HEADER);
        if (requestId != null) {
            problem.setProperty("requestId", requestId);
        }
        return problem;
    }

    private String message(String code, String part) {
        return messages.getMessage("problem." + code + "." + part, null, TURKISH);
    }
}
