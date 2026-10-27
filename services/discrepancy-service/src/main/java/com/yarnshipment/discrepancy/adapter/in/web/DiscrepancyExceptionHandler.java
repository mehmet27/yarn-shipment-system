package com.yarnshipment.discrepancy.adapter.in.web;

import com.yarnshipment.common.web.problem.ProblemFactory;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyNotFoundException;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyNotOpenException;
import com.yarnshipment.discrepancy.domain.model.LiablePartyRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Servise özgü hataların ERR-07 cevabı; ortak hatalar common-web, güvenlik hataları common-security tarafındadır. */
/* Spring Boot'un genel ProblemDetail işleyicisinden önce çalışır; aksi halde cevap code alanı olmadan döner. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public final class DiscrepancyExceptionHandler {

    private final ProblemFactory problems;

    public DiscrepancyExceptionHandler(ProblemFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(DiscrepancyNotFoundException.class)
    public ProblemDetail handleNotFound(HttpServletRequest request) {
        return problems.create(request, HttpStatus.NOT_FOUND, "DISCREPANCY_NOT_FOUND");
    }

    @ExceptionHandler(DiscrepancyNotOpenException.class)
    public ProblemDetail handleNotOpen(HttpServletRequest request) {
        return problems.create(request, HttpStatus.CONFLICT, "DISCREPANCY_NOT_OPEN");
    }

    @ExceptionHandler(LiablePartyRequiredException.class)
    public ProblemDetail handleLiablePartyRequired(HttpServletRequest request) {
        return problems.create(request, HttpStatus.BAD_REQUEST, "LIABLE_PARTY_REQUIRED");
    }
}
