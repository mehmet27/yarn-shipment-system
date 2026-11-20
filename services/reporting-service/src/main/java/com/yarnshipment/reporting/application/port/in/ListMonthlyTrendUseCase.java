package com.yarnshipment.reporting.application.port.in;

import com.yarnshipment.reporting.domain.model.MonthlyFlow;
import java.util.List;

/** Aylık giden iplik ↔ gelen kumaş eğilimi; eskiden yeniye sıralı. */
public interface ListMonthlyTrendUseCase {

    int DEFAULT_MONTHS = 6;
    int MAX_MONTHS = 12;

    /**
     * {@code query.to()} (boşsa bugün) ayında biten {@code months} takvim ayı; {@code query.from()} kullanılmaz.
     * Verisi olmayan ay sıfırlarla döner. {@code months} 1 ile {@link #MAX_MONTHS} arasında olmalıdır.
     */
    List<MonthlyFlow> listMonthlyTrend(ReportQuery query, int months);
}
