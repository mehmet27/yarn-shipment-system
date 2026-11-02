package com.yarnshipment.bag.application.service;

import com.yarnshipment.bag.application.port.in.RecordLotQualityLockCommand;
import com.yarnshipment.bag.application.port.in.RecordLotQualityLockUseCase;
import com.yarnshipment.bag.application.port.out.LotQualityLockPort;
import com.yarnshipment.bag.application.port.out.ProcessedEventPort;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code LotQualityLocked} olayını {@code lot_quality_lock} tablosuna yazar. Olay kimliği aynı transaction'da
 * işlendi sayılır; tekrar gelen olay ve zaten kayıtlı parti sessizce atlanır (EVT-05).
 */
@Service
public class RecordLotQualityLockService implements RecordLotQualityLockUseCase {

    private final LotQualityLockPort locks;
    private final ProcessedEventPort processedEvents;
    private final Clock clock;

    public RecordLotQualityLockService(LotQualityLockPort locks, ProcessedEventPort processedEvents, Clock clock) {
        this.locks = locks;
        this.processedEvents = processedEvents;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void recordLotQualityLock(RecordLotQualityLockCommand command) {
        if (!processedEvents.markProcessed(command.eventId(), command.eventType())) {
            return;
        }
        locks.insertIfAbsent(command.lock(), clock.instant());
    }
}
