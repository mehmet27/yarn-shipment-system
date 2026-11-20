package com.yarnshipment.reporting.application.service;

import com.yarnshipment.reporting.application.port.in.ListMonthlyTrendUseCase;
import com.yarnshipment.reporting.application.port.in.ReportQuery;
import com.yarnshipment.reporting.application.port.out.BalanceFirePort;
import com.yarnshipment.reporting.application.port.out.ReportScope;
import com.yarnshipment.reporting.domain.model.InvalidReportPeriodException;
import com.yarnshipment.reporting.domain.model.MonthlyFlow;
import com.yarnshipment.reporting.domain.model.ReportPeriod;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aylık eğilim: her ay için fire raporuyla aynı sorgu ({@link BalanceFirePort#findFireFiles}) çalışır, böylece aylık
 * sütunlar "Giden iplik ↔ gelen kumaş" raporuyla birebir tutar. Kapsam dışı kullanıcı sıfırlı aylar alır.
 */
@Service
public class MonthlyTrendService implements ListMonthlyTrendUseCase {

    private final BalanceFirePort port;
    private final ReportAccess access;
    private final Clock clock;

    MonthlyTrendService(BalanceFirePort port, ReportAccess access, Clock clock) {
        this.port = port;
        this.access = access;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyFlow> listMonthlyTrend(ReportQuery query, int months) {
        if (months < 1 || months > MAX_MONTHS) {
            throw new InvalidReportPeriodException("months must be between 1 and " + MAX_MONTHS);
        }
        LocalDate today = LocalDate.ofInstant(clock.instant(), ReportPeriod.ZONE);
        YearMonth last = YearMonth.from(query.to() != null ? query.to() : today);
        Optional<ReportScope> scope = access.scopeOf(query);
        return IntStream.rangeClosed(1, months)
                .mapToObj(index -> last.minusMonths((long) months - index))
                .map(month ->
                        MonthlyFlow.of(month, scope.map(s -> filesOf(month, s)).orElseGet(List::of)))
                .toList();
    }

    private List<com.yarnshipment.reporting.domain.model.FireFile> filesOf(YearMonth month, ReportScope scope) {
        return port.findFireFiles(new ReportPeriod(month.atDay(1), month.atEndOfMonth()), scope, null);
    }
}
