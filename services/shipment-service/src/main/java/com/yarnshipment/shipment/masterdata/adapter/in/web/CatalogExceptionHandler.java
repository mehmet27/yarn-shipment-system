package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.common.web.problem.ProblemFactory;
import com.yarnshipment.shipment.masterdata.domain.catalog.DuplicateCodeException;
import com.yarnshipment.shipment.masterdata.domain.lot.LotQualityLockedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** İplik, kalite sınıfı ve parti hatalarının ERR-07 cevabı; diğer ana veri hataları sevkiyat advice'ındadır. */
/* Spring Boot'un genel ProblemDetail işleyicisinden önce çalışır; aksi halde cevap code alanı olmadan döner. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public final class CatalogExceptionHandler {

    private final ProblemFactory problems;

    public CatalogExceptionHandler(ProblemFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(DuplicateCodeException.class)
    public ProblemDetail handleDuplicateCode(DuplicateCodeException exception, HttpServletRequest request) {
        ProblemDetail problem = problems.create(request, HttpStatus.CONFLICT, "DUPLICATE_CODE");
        problem.setProperty("errors", List.of(Map.of("field", exception.field(), "message", "duplicate")));
        return problem;
    }

    @ExceptionHandler(LotQualityLockedException.class)
    public ProblemDetail handleLotQualityLocked(HttpServletRequest request) {
        return problems.create(request, HttpStatus.CONFLICT, "LOT_QUALITY_LOCKED");
    }
}
