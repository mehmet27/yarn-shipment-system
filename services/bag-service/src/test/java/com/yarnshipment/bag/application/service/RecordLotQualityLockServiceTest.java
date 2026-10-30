package com.yarnshipment.bag.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.bag.application.port.in.RecordLotQualityLockCommand;
import com.yarnshipment.bag.application.port.out.LotQualityLockPort;
import com.yarnshipment.bag.application.port.out.ProcessedEventPort;
import com.yarnshipment.bag.domain.model.LotQualityLock;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecordLotQualityLockServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-30T10:00:05Z");
    private static final UUID EVENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000000e1");
    private static final String TYPE = "com.yarnshipment.masterdata.lot-quality-locked.v1";
    private static final LotQualityLock LOCK = new LotQualityLock(
            UUID.fromString("0192f0a0-0000-7000-8000-000000000103"),
            UUID.fromString("0192f0a0-0000-7000-8000-000000000001"),
            "P-2026-0003",
            UUID.fromString("0192f0a0-0000-7000-8000-000000000202"),
            "A",
            Instant.parse("2026-10-30T10:00:00Z"));

    private final LotQualityLockPort locks = mock(LotQualityLockPort.class);
    private final ProcessedEventPort processed = mock(ProcessedEventPort.class);
    private final RecordLotQualityLockService service =
            new RecordLotQualityLockService(locks, processed, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("İlk kez gelen kilit kaydedilir")
    void shouldRecordFirstDelivery() {
        when(processed.markProcessed(EVENT, TYPE)).thenReturn(true);

        service.recordLotQualityLock(new RecordLotQualityLockCommand(EVENT, TYPE, LOCK));

        verify(locks).insertIfAbsent(LOCK, NOW);
    }

    @Test
    @DisplayName("Tekrar gelen olay yeniden yazılmaz (EVT-05)")
    void shouldSkipRedelivery() {
        when(processed.markProcessed(EVENT, TYPE)).thenReturn(false);

        service.recordLotQualityLock(new RecordLotQualityLockCommand(EVENT, TYPE, LOCK));

        verify(locks, never()).insertIfAbsent(any(), any());
    }
}
