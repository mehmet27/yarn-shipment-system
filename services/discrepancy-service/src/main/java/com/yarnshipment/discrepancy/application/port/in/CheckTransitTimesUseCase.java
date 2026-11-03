package com.yarnshipment.discrepancy.application.port.in;

/** Sınırdan uzun süredir yolda olan sevkiyatlar için zaman uyuşmazlığı açar (zamanlanmış kontrol). */
public interface CheckTransitTimesUseCase {

    /** Açılan uyuşmazlık sayısı. */
    int checkTransitTimes();
}
