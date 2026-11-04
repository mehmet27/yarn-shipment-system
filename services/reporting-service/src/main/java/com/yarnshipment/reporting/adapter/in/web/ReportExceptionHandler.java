package com.yarnshipment.reporting.adapter.in.web;

import com.yarnshipment.common.web.problem.ProblemFactory;
import com.yarnshipment.reporting.domain.model.InvalidReportPeriodException;
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
public final class ReportExceptionHandler {

    private final ProblemFactory problems;

    public ReportExceptionHandler(ProblemFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(InvalidReportPeriodException.class)
    public ProblemDetail handleInvalidPeriod(HttpServletRequest request) {
        return problems.create(request, HttpStatus.BAD_REQUEST, "REPORT_PERIOD_INVALID");
    }
}
