package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.common.web.problem.ProblemFactory;
import com.yarnshipment.shipment.application.port.in.ShipmentNotFoundException;
import com.yarnshipment.shipment.domain.bag.BagReservationException;
import com.yarnshipment.shipment.domain.bag.BagServiceUnavailableException;
import com.yarnshipment.shipment.domain.model.InvalidDeliveryException;
import com.yarnshipment.shipment.domain.model.InvalidDispatchException;
import com.yarnshipment.shipment.domain.model.InvalidNewShipmentException;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.SealAlreadyUsedException;
import com.yarnshipment.shipment.masterdata.domain.model.DuplicatePlateException;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Servise özgü hataların ERR-07 cevabı. Eksik başlık ve sınır ihlali gibi ortak hatalar {@code common-web}
 * advice'ında, güvenlik hataları (401/403) {@code common-security} tarafında üretilir.
 */
/* Spring Boot'un genel ProblemDetail işleyicisinden önce çalışır; aksi halde cevap code alanı olmadan döner. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public final class ShipmentExceptionHandler {

    private final ProblemFactory problems;

    public ShipmentExceptionHandler(ProblemFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(ShipmentNotFoundException.class)
    public ProblemDetail handleNotFound(HttpServletRequest request) {
        return problem(request, HttpStatus.NOT_FOUND, "SHIPMENT_NOT_FOUND");
    }

    @ExceptionHandler(InvalidShipmentStatusException.class)
    public ProblemDetail handleInvalidStatus(HttpServletRequest request) {
        return problem(request, HttpStatus.CONFLICT, "INVALID_SHIPMENT_STATUS");
    }

    @ExceptionHandler(InvalidDeliveryException.class)
    public ProblemDetail handleInvalidDelivery(InvalidDeliveryException exception, HttpServletRequest request) {
        ProblemDetail problem = problem(request, HttpStatus.BAD_REQUEST, "INVALID_DELIVERY");
        problem.setProperty("errors", List.of(Map.of("field", exception.field(), "message", "invalid")));
        return problem;
    }

    @ExceptionHandler(InvalidDispatchException.class)
    public ProblemDetail handleInvalidDispatch(InvalidDispatchException exception, HttpServletRequest request) {
        ProblemDetail problem = problem(request, HttpStatus.BAD_REQUEST, "INVALID_DISPATCH");
        problem.setProperty("errors", List.of(Map.of("field", exception.field(), "message", "invalid")));
        return problem;
    }

    @ExceptionHandler(InvalidNewShipmentException.class)
    public ProblemDetail handleInvalidNewShipment(InvalidNewShipmentException exception, HttpServletRequest request) {
        ProblemDetail problem = problem(request, HttpStatus.BAD_REQUEST, "INVALID_SHIPMENT");
        problem.setProperty("errors", List.of(Map.of("field", exception.field(), "message", "invalid")));
        return problem;
    }

    @ExceptionHandler(SealAlreadyUsedException.class)
    public ProblemDetail handleSealAlreadyUsed(HttpServletRequest request) {
        return problem(request, HttpStatus.CONFLICT, "SEAL_ALREADY_USED");
    }

    @ExceptionHandler(BagReservationException.class)
    public ProblemDetail handleBagReservation(BagReservationException exception, HttpServletRequest request) {
        return switch (exception.reason()) {
            case NOT_FOUND -> problem(request, HttpStatus.NOT_FOUND, "BAG_NOT_FOUND");
            case NOT_AVAILABLE -> problem(request, HttpStatus.CONFLICT, "BAG_NOT_AVAILABLE");
            case ALREADY_RESERVED -> problem(request, HttpStatus.CONFLICT, "BAG_ALREADY_RESERVED");
        };
    }

    @ExceptionHandler(BagServiceUnavailableException.class)
    public ProblemDetail handleBagServiceUnavailable(HttpServletRequest request) {
        return problem(request, HttpStatus.BAD_GATEWAY, "BAG_SERVICE_UNAVAILABLE");
    }

    @ExceptionHandler(InvalidMasterDataException.class)
    public ProblemDetail handleInvalidMasterData(InvalidMasterDataException exception, HttpServletRequest request) {
        ProblemDetail problem = problem(request, HttpStatus.BAD_REQUEST, "INVALID_MASTER_DATA");
        problem.setProperty("errors", List.of(Map.of("field", exception.field(), "message", "invalid")));
        return problem;
    }

    @ExceptionHandler(MasterDataNotFoundException.class)
    public ProblemDetail handleMasterDataNotFound(HttpServletRequest request) {
        return problem(request, HttpStatus.NOT_FOUND, "MASTER_DATA_NOT_FOUND");
    }

    @ExceptionHandler(DuplicatePlateException.class)
    public ProblemDetail handleDuplicatePlate(HttpServletRequest request) {
        return problem(request, HttpStatus.CONFLICT, "DUPLICATE_PLATE");
    }

    private ProblemDetail problem(HttpServletRequest request, HttpStatus status, String code) {
        return problems.create(request, status, code);
    }
}
