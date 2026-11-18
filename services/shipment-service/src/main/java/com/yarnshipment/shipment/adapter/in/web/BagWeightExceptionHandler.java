package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.common.web.problem.ProblemFactory;
import com.yarnshipment.shipment.domain.weight.BagWeightMismatchException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Çuval toplamı ile araç net kilosunun tutmaması hatasının ERR-07 cevabı. */
/* Spring Boot'un genel ProblemDetail işleyicisinden önce çalışır; aksi halde cevap code alanı olmadan döner. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public final class BagWeightExceptionHandler {

    private final ProblemFactory problems;

    public BagWeightExceptionHandler(ProblemFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(BagWeightMismatchException.class)
    public ProblemDetail handleBagWeightMismatch(BagWeightMismatchException exception, HttpServletRequest request) {
        ProblemDetail problem = problems.create(request, HttpStatus.CONFLICT, "BAG_WEIGHT_MISMATCH");
        problem.setProperty("bagKg", exception.bagKg().toPlainString());
        problem.setProperty("measuredKg", exception.measuredKg().toPlainString());
        problem.setProperty("toleranceKg", exception.toleranceKg().toPlainString());
        return problem;
    }
}
