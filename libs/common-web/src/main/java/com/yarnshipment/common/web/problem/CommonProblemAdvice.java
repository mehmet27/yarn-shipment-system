package com.yarnshipment.common.web.problem;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Tüm servislerde aynı olan istek hataları (ERR-06): eksik başlık, sınır ihlali ve okunamayan gövde. Servise özgü
 * iş hataları servisin kendi advice'ında kalır ve {@link ProblemFactory} ile aynı biçimi üretir.
 */
/* Spring Boot'un genel ProblemDetail işleyicisinden önce çalışır; aksi halde cevap code alanı olmadan döner. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public final class CommonProblemAdvice {

    private final ProblemFactory problems;

    public CommonProblemAdvice(ProblemFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ProblemDetail handleMissingHeader(MissingRequestHeaderException exception, HttpServletRequest request) {
        ProblemDetail problem = problems.create(request, HttpStatus.BAD_REQUEST, "MISSING_HEADER");
        problem.setProperty("errors", List.of(Map.of("field", exception.getHeaderName(), "message", "required")));
        return problem;
    }

    /** Sözleşmedeki sınırlar (ör. {@code size <= 100}) {@code @Validated} arayüzde denetlenir (API-07). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
        ProblemDetail problem = problems.create(request, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
        List<Map<String, String>> errors = exception.getConstraintViolations().stream()
                .map(violation -> Map.of(
                        "field", lastNode(violation.getPropertyPath().toString()), "message", violation.getMessage()))
                .toList();
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpServletRequest request) {
        return problems.create(request, HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST");
    }

    private static String lastNode(String path) {
        return path.substring(path.lastIndexOf('.') + 1);
    }
}
