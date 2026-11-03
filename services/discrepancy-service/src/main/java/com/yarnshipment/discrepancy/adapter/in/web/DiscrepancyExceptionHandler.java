package com.yarnshipment.discrepancy.adapter.in.web;

import com.yarnshipment.common.web.problem.ProblemFactory;
import com.yarnshipment.discrepancy.domain.claim.ClaimRejectedException;
import com.yarnshipment.discrepancy.domain.claim.EvidenceServiceUnavailableException;
import com.yarnshipment.discrepancy.domain.claim.InspectionNotFoundException;
import com.yarnshipment.discrepancy.domain.claim.QualityClaimNotFoundException;
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

    @ExceptionHandler(QualityClaimNotFoundException.class)
    public ProblemDetail handleClaimNotFound(HttpServletRequest request) {
        return problems.create(request, HttpStatus.NOT_FOUND, "QUALITY_CLAIM_NOT_FOUND");
    }

    @ExceptionHandler(InspectionNotFoundException.class)
    public ProblemDetail handleInspectionNotFound(HttpServletRequest request) {
        return problems.create(request, HttpStatus.NOT_FOUND, "INSPECTION_NOT_FOUND");
    }

    @ExceptionHandler(ClaimRejectedException.class)
    public ProblemDetail handleClaimRejected(ClaimRejectedException exception, HttpServletRequest request) {
        HttpStatus status = switch (exception.rejection()) {
            case BAG_NOT_DELIVERED, EVIDENCE_REQUIRED, EVIDENCE_INVALID, RESULT_EVIDENCE_REQUIRED ->
                HttpStatus.BAD_REQUEST;
            case NOT_AVAILABLE, WINDOW_CLOSED, BAG_ALREADY_CLAIMED, INSPECTION_RECORDED -> HttpStatus.CONFLICT;
        };
        return problems.create(
                request, status, "QUALITY_CLAIM_" + exception.rejection().name());
    }

    @ExceptionHandler(EvidenceServiceUnavailableException.class)
    public ProblemDetail handleEvidenceUnavailable(HttpServletRequest request) {
        return problems.create(request, HttpStatus.SERVICE_UNAVAILABLE, "EVIDENCE_SERVICE_UNAVAILABLE");
    }
}
