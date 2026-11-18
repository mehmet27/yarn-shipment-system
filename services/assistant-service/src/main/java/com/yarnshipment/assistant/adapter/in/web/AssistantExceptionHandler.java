package com.yarnshipment.assistant.adapter.in.web;

import com.yarnshipment.assistant.domain.model.HelpRateLimitedException;
import com.yarnshipment.assistant.domain.model.HelpUnavailableException;
import com.yarnshipment.assistant.domain.model.InvalidHelpQuestionException;
import com.yarnshipment.common.web.problem.ProblemFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Servise özgü hataların ERR-07 cevabı. */
/* Spring Boot'un genel ProblemDetail işleyicisinden önce çalışır; aksi halde cevap code alanı olmadan döner. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public final class AssistantExceptionHandler {

    private final ProblemFactory problems;

    public AssistantExceptionHandler(ProblemFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(InvalidHelpQuestionException.class)
    public ProblemDetail handleInvalidQuestion(HttpServletRequest request) {
        return problems.create(request, HttpStatus.BAD_REQUEST, "HELP_QUESTION_INVALID");
    }

    @ExceptionHandler(HelpRateLimitedException.class)
    public ProblemDetail handleRateLimited(HttpServletRequest request) {
        return problems.create(request, HttpStatus.TOO_MANY_REQUESTS, "HELP_RATE_LIMITED");
    }

    @ExceptionHandler(HelpUnavailableException.class)
    public ProblemDetail handleUnavailable(HttpServletRequest request) {
        return problems.create(request, HttpStatus.SERVICE_UNAVAILABLE, "HELP_UNAVAILABLE");
    }
}
