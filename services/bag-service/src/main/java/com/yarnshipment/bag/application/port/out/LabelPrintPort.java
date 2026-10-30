package com.yarnshipment.bag.application.port.out;

import com.yarnshipment.bag.domain.model.BagLabel;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Etiket baskı kayıtları ({@code label_print}); kayıtlar silinmez ve değişmez. */
public interface LabelPrintPort {

    /**
     * Çuvalları transaction sonuna kadar kilitler ve her birinin şimdiye kadarki baskı sayısını döner. Kilit, aynı
     * çuvalın iki eşzamanlı baskısının aynı baskı numarasını almasını önler.
     */
    Map<UUID, Integer> lockAndCountPrints(Collection<UUID> bagIds);

    /** Baskıları kaydeder; {@code reprintReason} yalnızca yeniden baskı satırlarına yazılır. */
    void record(List<BagLabel> labels, @Nullable String reprintReason, UUID actorUserId, Instant at);
}
