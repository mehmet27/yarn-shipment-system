package com.yarnshipment.evidence.adapter.in.web;

import com.yarnshipment.common.web.problem.ProblemFactory;
import com.yarnshipment.evidence.domain.model.ShipmentLockedException;
import com.yarnshipment.evidence.domain.model.ShipmentNotAccessibleException;
import com.yarnshipment.evidence.domain.model.ShipmentServiceUnavailableException;
import com.yarnshipment.evidence.domain.model.UploadIntentNotFoundException;
import com.yarnshipment.evidence.domain.model.UploadMismatchException;
import com.yarnshipment.evidence.domain.model.UploadNotAllowedException;
import com.yarnshipment.evidence.domain.model.UploadNotCompletedException;
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
public final class EvidenceExceptionHandler {

    private final ProblemFactory problems;

    public EvidenceExceptionHandler(ProblemFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(UploadNotAllowedException.class)
    public ProblemDetail handleNotAllowed(HttpServletRequest request) {
        return problems.create(request, HttpStatus.BAD_REQUEST, "UPLOAD_NOT_ALLOWED");
    }

    @ExceptionHandler(UploadIntentNotFoundException.class)
    public ProblemDetail handleIntentNotFound(HttpServletRequest request) {
        return problems.create(request, HttpStatus.NOT_FOUND, "UPLOAD_NOT_FOUND");
    }

    @ExceptionHandler(UploadNotCompletedException.class)
    public ProblemDetail handleNotCompleted(HttpServletRequest request) {
        return problems.create(request, HttpStatus.CONFLICT, "UPLOAD_NOT_COMPLETED");
    }

    @ExceptionHandler(UploadMismatchException.class)
    public ProblemDetail handleMismatch(HttpServletRequest request) {
        return problems.create(request, HttpStatus.UNPROCESSABLE_CONTENT, "UPLOAD_MISMATCH");
    }

    @ExceptionHandler(ShipmentNotAccessibleException.class)
    public ProblemDetail handleShipmentNotAccessible(HttpServletRequest request) {
        return problems.create(request, HttpStatus.NOT_FOUND, "SHIPMENT_NOT_FOUND");
    }

    @ExceptionHandler(ShipmentLockedException.class)
    public ProblemDetail handleShipmentLocked(HttpServletRequest request) {
        return problems.create(request, HttpStatus.CONFLICT, "SHIPMENT_LOCKED");
    }

    @ExceptionHandler(ShipmentServiceUnavailableException.class)
    public ProblemDetail handleShipmentUnavailable(HttpServletRequest request) {
        return problems.create(request, HttpStatus.SERVICE_UNAVAILABLE, "SHIPMENT_SERVICE_UNAVAILABLE");
    }
}
