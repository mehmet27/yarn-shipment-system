package com.yarnshipment.bag.application.port.out;

import com.yarnshipment.bag.domain.model.Bag;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Çuval rezervasyonu yazma portu; tek kullanım garantisi veritabanı işleminde verilir. */
public interface BagReservationPort {

    /** Çuvalları sevkiyata ayırır; herhangi biri ayrılamazsa istisna fırlatır ve hiçbiri ayrılmaz. */
    void reserve(UUID shipmentId, List<Bag> bags, UUID actorUserId, Instant at);

    /** Sevkiyatın açık rezervasyonlarını bırakır; bırakılan sayıyı döner. */
    int release(UUID shipmentId, String reason, UUID actorUserId, Instant at);
}
