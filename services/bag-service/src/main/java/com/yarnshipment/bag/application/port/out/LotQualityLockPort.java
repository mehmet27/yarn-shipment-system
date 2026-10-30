package com.yarnshipment.bag.application.port.out;

import com.yarnshipment.bag.domain.model.LotQualityLock;
import java.time.Instant;

/** Parti kalite kilidinin yerel kopyası ({@code lot_quality_lock}); yalnızca eklenir. */
public interface LotQualityLockPort {

    /** Kilidi ekler; parti zaten kayıtlıysa bir şey yapmaz ve {@code false} döner. */
    boolean insertIfAbsent(LotQualityLock lock, Instant recordedAt);
}
