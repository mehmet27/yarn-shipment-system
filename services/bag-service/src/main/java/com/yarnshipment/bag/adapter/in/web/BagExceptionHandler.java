package com.yarnshipment.bag.adapter.in.web;

import com.yarnshipment.bag.application.port.in.BagNotFoundException;
import com.yarnshipment.bag.domain.model.BagAlreadyReservedException;
import com.yarnshipment.bag.domain.model.BagNotAvailableException;
import com.yarnshipment.common.web.problem.ProblemFactory;
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
public final class BagExceptionHandler {

    private final ProblemFactory problems;

    public BagExceptionHandler(ProblemFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(BagNotFoundException.class)
    public ProblemDetail handleNotFound(HttpServletRequest request) {
        return problem(request, HttpStatus.NOT_FOUND, "BAG_NOT_FOUND");
    }

    @ExceptionHandler(BagNotAvailableException.class)
    public ProblemDetail handleNotAvailable(HttpServletRequest request) {
        return problem(request, HttpStatus.CONFLICT, "BAG_NOT_AVAILABLE");
    }

    @ExceptionHandler(BagAlreadyReservedException.class)
    public ProblemDetail handleAlreadyReserved(HttpServletRequest request) {
        return problem(request, HttpStatus.CONFLICT, "BAG_ALREADY_RESERVED");
    }

    private ProblemDetail problem(HttpServletRequest request, HttpStatus status, String code) {
        return problems.create(request, status, code);
    }
}
