package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.common.web.problem.ProblemFactory;
import com.yarnshipment.shipment.domain.evidence.EvidenceServiceUnavailableException;
import com.yarnshipment.shipment.domain.evidence.InvalidWaybillEvidenceException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Tamamlamadaki irsaliye belgesine ilişkin hataların ERR-07 cevabı. */
/* Spring Boot'un genel ProblemDetail işleyicisinden önce çalışır; aksi halde cevap code alanı olmadan döner. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public final class EvidenceExceptionHandler {

    private final ProblemFactory problems;

    public EvidenceExceptionHandler(ProblemFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(InvalidWaybillEvidenceException.class)
    public ProblemDetail handleInvalidWaybillEvidence(HttpServletRequest request) {
        return problems.create(request, HttpStatus.UNPROCESSABLE_CONTENT, "WAYBILL_EVIDENCE_INVALID");
    }

    @ExceptionHandler(EvidenceServiceUnavailableException.class)
    public ProblemDetail handleEvidenceServiceUnavailable(HttpServletRequest request) {
        return problems.create(request, HttpStatus.BAD_GATEWAY, "EVIDENCE_SERVICE_UNAVAILABLE");
    }
}
