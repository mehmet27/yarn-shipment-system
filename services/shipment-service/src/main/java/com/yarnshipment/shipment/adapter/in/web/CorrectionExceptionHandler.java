package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.common.web.problem.ProblemFactory;
import com.yarnshipment.shipment.domain.correction.CorrectionConflictException;
import com.yarnshipment.shipment.domain.correction.InvalidCorrectionException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Gerekçeli düzeltmeye ilişkin hataların ERR-07 cevabı. */
/* Spring Boot'un genel ProblemDetail işleyicisinden önce çalışır; aksi halde cevap code alanı olmadan döner. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public final class CorrectionExceptionHandler {

    private final ProblemFactory problems;

    public CorrectionExceptionHandler(ProblemFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(InvalidCorrectionException.class)
    public ProblemDetail handleInvalidCorrection(InvalidCorrectionException exception, HttpServletRequest request) {
        ProblemDetail problem = problems.create(request, HttpStatus.BAD_REQUEST, "INVALID_CORRECTION");
        problem.setProperty("errors", List.of(Map.of("field", exception.field(), "message", "invalid")));
        return problem;
    }

    @ExceptionHandler(CorrectionConflictException.class)
    public ProblemDetail handleCorrectionConflict(HttpServletRequest request) {
        return problems.create(request, HttpStatus.CONFLICT, "CORRECTION_CONFLICT");
    }
}
