package com.yarnshipment.shipment.application.port.out.ledger;

import com.yarnshipment.shipment.domain.stock.WorkArea;
import java.util.Optional;
import java.util.UUID;

/** Kullanıcıların çalışma alanı kayıtları. */
public interface WorkAreaPort {

    /** Kullanıcıya çalışma alanı atanmamışsa boş döner. */
    Optional<WorkArea> findByUser(UUID userId);
}
