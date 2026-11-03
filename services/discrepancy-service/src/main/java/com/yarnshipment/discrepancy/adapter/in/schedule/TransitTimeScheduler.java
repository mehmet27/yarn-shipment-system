package com.yarnshipment.discrepancy.adapter.in.schedule;

import com.yarnshipment.discrepancy.application.port.in.CheckTransitTimesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Yolda kalma süresini düzenli aralıkla kontrol eder. Birden çok kopya aynı anda çalışsa da her sevkiyatta tek
 * uyuşmazlık açılır (izleme kaydındaki işaret satır kilidiyle yazılır).
 */
@Component
final class TransitTimeScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(TransitTimeScheduler.class);

    private final CheckTransitTimesUseCase checkTransitTimesUseCase;

    TransitTimeScheduler(CheckTransitTimesUseCase checkTransitTimesUseCase) {
        this.checkTransitTimesUseCase = checkTransitTimesUseCase;
    }

    @Scheduled(
            initialDelayString = "${yarnshipment.discrepancy.transit-check.initial-delay:PT1M}",
            fixedDelayString = "${yarnshipment.discrepancy.transit-check.interval:PT15M}")
    void checkTransitTimes() {
        int opened = checkTransitTimesUseCase.checkTransitTimes();
        if (opened > 0) {
            LOG.info("Transit time discrepancies opened count={}", opened);
        }
    }
}
