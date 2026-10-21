package com.yarnshipment.shipment.application.port.out;

import com.yarnshipment.shipment.domain.model.DispatchRecording;
import java.util.UUID;

/** Çıkış kaydı yazma portu; tartım, mühür, durum geçişi ve olay tek transaction'dadır. */
public interface DispatchCommandPort {

    /** Sevkiyat için çıkış tartımı daha önce kaydedildi mi (yeniden deneme). */
    boolean hasDispatchWeighing(UUID shipmentId);

    /**
     * Arada durum değiştiyse {@code InvalidShipmentStatusException}, mühür başka sevkiyatta kullanılmışsa
     * {@code SealAlreadyUsedException}.
     *
     * @param actorUserId işlemi yapan kullanıcı (audit ve geçmiş)
     */
    void recordDispatch(DispatchRecording recording, UUID actorUserId);
}
