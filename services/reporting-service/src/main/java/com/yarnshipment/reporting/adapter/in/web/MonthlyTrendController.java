package com.yarnshipment.reporting.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.reporting.adapter.in.web.api.TrendApi;
import com.yarnshipment.reporting.adapter.in.web.api.model.MonthlyFlowView;
import com.yarnshipment.reporting.application.port.in.ListMonthlyTrendUseCase;
import com.yarnshipment.reporting.application.port.in.ReportQuery;
import com.yarnshipment.reporting.domain.model.MonthlyFlow;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Aylık giden iplik ↔ gelen kumaş eğilimi ucu. Method security proxy'si için final değildir (SEC-02). */
@RestController
public class MonthlyTrendController implements TrendApi {

    private final ListMonthlyTrendUseCase listMonthlyTrend;
    private final CurrentActorProvider currentActorProvider;

    public MonthlyTrendController(ListMonthlyTrendUseCase listMonthlyTrend, CurrentActorProvider currentActorProvider) {
        this.listMonthlyTrend = listMonthlyTrend;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<List<MonthlyFlowView>> getMonthlyTrend(
            @Nullable LocalDate to, @Nullable UUID siteId, @Nullable Integer months) {
        ReportQuery query = new ReportQuery(currentActorProvider.getCurrentActor(), null, to, siteId);
        int count = months == null ? ListMonthlyTrendUseCase.DEFAULT_MONTHS : months;
        return ResponseEntity.ok(listMonthlyTrend.listMonthlyTrend(query, count).stream()
                .map(MonthlyTrendController::toView)
                .toList());
    }

    private static MonthlyFlowView toView(MonthlyFlow flow) {
        return new MonthlyFlowView(
                flow.month().toString(),
                flow.month().atDay(1),
                flow.month().atEndOfMonth(),
                flow.shipmentCount(),
                flow.sentKg().toPlainString(),
                flow.returnedYarnKg().toPlainString(),
                flow.fabricKg().toPlainString(),
                flow.actualFireRate().toPlainString(),
                flow.shortageKg().toPlainString());
    }
}
