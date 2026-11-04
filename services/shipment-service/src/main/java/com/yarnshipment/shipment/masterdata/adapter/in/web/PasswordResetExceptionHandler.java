package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.common.web.problem.ProblemFactory;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidResetCodeException;
import com.yarnshipment.shipment.masterdata.domain.model.SmsUnavailableException;
import com.yarnshipment.shipment.masterdata.domain.model.WeakPasswordException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Şifre sıfırlama ve SMS hatalarının ERR-07 cevabı. */
/* Spring Boot'un genel ProblemDetail işleyicisinden önce çalışır; aksi halde cevap code alanı olmadan döner. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public final class PasswordResetExceptionHandler {

    private final ProblemFactory problems;

    public PasswordResetExceptionHandler(ProblemFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(InvalidResetCodeException.class)
    public ProblemDetail handleInvalidCode(HttpServletRequest request) {
        return problems.create(request, HttpStatus.BAD_REQUEST, "INVALID_RESET_CODE");
    }

    @ExceptionHandler(WeakPasswordException.class)
    public ProblemDetail handleWeakPassword(HttpServletRequest request) {
        return problems.create(request, HttpStatus.BAD_REQUEST, "WEAK_PASSWORD");
    }

    @ExceptionHandler(SmsUnavailableException.class)
    public ProblemDetail handleSmsUnavailable(HttpServletRequest request) {
        return problems.create(request, HttpStatus.BAD_GATEWAY, "SMS_UNAVAILABLE");
    }
}
