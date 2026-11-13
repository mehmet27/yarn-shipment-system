package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.common.web.problem.ProblemFactory;
import com.yarnshipment.shipment.masterdata.domain.model.AccountLockedException;
import com.yarnshipment.shipment.masterdata.domain.model.AuthenticationFailedException;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidCurrentPasswordException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Giriş ve şifre değiştirme hatalarının ERR-07 cevabı. */
/* Spring Boot'un genel ProblemDetail işleyicisinden önce çalışır; aksi halde cevap code alanı olmadan döner. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public final class AuthenticationExceptionHandler {

    private final ProblemFactory problems;

    public AuthenticationExceptionHandler(ProblemFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ProblemDetail handleAuthenticationFailed(HttpServletRequest request) {
        return problems.create(request, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED");
    }

    @ExceptionHandler(AccountLockedException.class)
    public ProblemDetail handleAccountLocked(HttpServletRequest request) {
        return problems.create(request, HttpStatus.LOCKED, "ACCOUNT_LOCKED");
    }

    @ExceptionHandler(InvalidCurrentPasswordException.class)
    public ProblemDetail handleInvalidCurrentPassword(HttpServletRequest request) {
        return problems.create(request, HttpStatus.BAD_REQUEST, "INVALID_CURRENT_PASSWORD");
    }
}
