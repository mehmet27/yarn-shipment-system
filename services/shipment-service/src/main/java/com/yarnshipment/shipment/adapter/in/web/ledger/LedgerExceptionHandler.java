package com.yarnshipment.shipment.adapter.in.web.ledger;

import com.yarnshipment.common.web.problem.ProblemFactory;
import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.fire.FireSettlementClosedException;
import com.yarnshipment.shipment.domain.fire.FireSettlementNotFoundException;
import com.yarnshipment.shipment.domain.fire.InvalidFireSettlementActionException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Bakiye, kumaş girişi, iplik dönüşü ve fire dosyası hatalarının ERR-07 cevabı. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.yarnshipment.shipment.adapter.in.web.ledger")
public final class LedgerExceptionHandler {

    private final ProblemFactory problems;

    public LedgerExceptionHandler(ProblemFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(InvalidLedgerInputException.class)
    public ProblemDetail handleInvalidInput(InvalidLedgerInputException exception, HttpServletRequest request) {
        ProblemDetail problem = problems.create(request, HttpStatus.BAD_REQUEST, "INVALID_LEDGER_INPUT");
        problem.setProperty("errors", List.of(Map.of("field", exception.field(), "message", "invalid")));
        return problem;
    }

    @ExceptionHandler(FireSettlementNotFoundException.class)
    public ProblemDetail handleNotFound(HttpServletRequest request) {
        return problems.create(request, HttpStatus.NOT_FOUND, "FIRE_SETTLEMENT_NOT_FOUND");
    }

    @ExceptionHandler(FireSettlementClosedException.class)
    public ProblemDetail handleClosed(HttpServletRequest request) {
        return problems.create(request, HttpStatus.CONFLICT, "FIRE_SETTLEMENT_CLOSED");
    }

    @ExceptionHandler(InvalidFireSettlementActionException.class)
    public ProblemDetail handleInvalidAction(
            InvalidFireSettlementActionException exception, HttpServletRequest request) {
        ProblemDetail problem = problems.create(request, HttpStatus.CONFLICT, "INVALID_FIRE_SETTLEMENT_ACTION");
        problem.setProperty("reason", exception.reason());
        return problem;
    }
}
