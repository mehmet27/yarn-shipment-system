package com.yarnshipment.common.web.problem;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Spring MVC'nin kendi istek hataları (bozuk kimlik, eksik parametre, yanlış yöntem, olmayan adres). Durum kodu
 * Spring'in varsayılanıyla aynıdır; yalnızca gövde ERR-07 biçimine ve Türkçe metne çevrilir, böylece istemciye
 * İngilizce ya da teknik metin (sınıf adı, parametre tipi) dönmez.
 */
/* Spring Boot'un genel ProblemDetail işleyicisinden önce çalışır; aksi halde cevap code alanı olmadan döner. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public final class MvcProblemAdvice {

    private static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    private static final String FIELD = "field";
    private static final String MESSAGE = "message";

    private final ProblemFactory problems;

    public MvcProblemAdvice(ProblemFactory problems) {
        this.problems = problems;
    }

    /** Gövdedeki {@code @Valid} ihlalleri; alan adları istemcinin gönderdiği adlardır. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleInvalidBody(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        FIELD, error.getField(), MESSAGE, Objects.requireNonNullElse(error.getDefaultMessage(), "")))
                .toList();
        return validationFailed(request, errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleInvalidArguments(HttpServletRequest request) {
        return validationFailed(request, List.of());
    }

    /** Ör. yol ya da sorgu parametresinde UUID yerine rastgele metin. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return validationFailed(request, List.of(Map.of(FIELD, exception.getName(), MESSAGE, "invalid")));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParameter(
            MissingServletRequestParameterException exception, HttpServletRequest request) {
        return validationFailed(request, List.of(Map.of(FIELD, exception.getParameterName(), MESSAGE, "required")));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(exception.getHeaders());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(headers)
                .body(problems.create(request, HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleUnsupportedMediaType(HttpServletRequest request) {
        return problems.create(request, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE");
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ProblemDetail handleUnknownPath(HttpServletRequest request) {
        return problems.create(request, HttpStatus.NOT_FOUND, "NOT_FOUND");
    }

    private ProblemDetail validationFailed(HttpServletRequest request, List<Map<String, String>> errors) {
        ProblemDetail problem = problems.create(request, HttpStatus.BAD_REQUEST, VALIDATION_FAILED);
        problem.setProperty("errors", errors);
        return problem;
    }
}
