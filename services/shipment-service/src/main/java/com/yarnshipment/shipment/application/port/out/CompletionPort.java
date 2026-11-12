package com.yarnshipment.shipment.application.port.out;

import com.yarnshipment.shipment.domain.completion.Completion;
import java.util.UUID;

/** Tamamlamayı yazar: irsaliye numarası, durum geçişi, kilit, geçmiş satırı ve olay yayını aynı transaction'da. */
public interface CompletionPort {

    /** Arada durum değiştiyse {@code InvalidShipmentStatusException}. */
    void complete(Completion completion, UUID actorUserId);
}
