package com.yarnshipment.discrepancy.domain.ticket;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Sevkiyatın karşılaştırmada kullanılan fiş okumaları: her taraf için en son güvenle okunan fiş.
 *
 * @param dispatch çıkış fişi; yoksa ya da okunamadıysa boş
 * @param delivery teslim fişi; yoksa ya da okunamadıysa boş
 * @param reuses bu fişlerin başka sevkiyatlarda da görüldüğü durumlar
 */
public record ShipmentTickets(
        @Nullable TicketReading dispatch, @Nullable TicketReading delivery, List<TicketReuse> reuses) {

    /** Okunmuş fiş yok; fiş kontrolleri atlanır. */
    public static final ShipmentTickets NONE = new ShipmentTickets(null, null, List.of());

    public ShipmentTickets {
        reuses = List.copyOf(reuses);
    }

    @Nullable
    public ScaleWeights dispatchWeights() {
        return dispatch == null ? null : dispatch.weights();
    }

    @Nullable
    public ScaleWeights deliveryWeights() {
        return delivery == null ? null : delivery.weights();
    }
}
