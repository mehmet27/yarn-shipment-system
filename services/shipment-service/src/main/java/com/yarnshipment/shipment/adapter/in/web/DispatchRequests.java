package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.shipment.adapter.in.web.api.model.DispatchWeighingRequest;
import com.yarnshipment.shipment.domain.model.DispatchMeasurement;
import com.yarnshipment.shipment.domain.model.InvalidDispatchException;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/** Çıkış tartımı isteğini domain değerlerine çevirir. Kilo dizeleri sözleşmedeki desenle zaten doğrulanmıştır. */
final class DispatchRequests {

    private DispatchRequests() {}

    static DispatchMeasurement toMeasurement(DispatchWeighingRequest request) {
        BigDecimal gross = either(request.getLoadedVehicleKg(), request.getGrossKg(), "loadedVehicleKg");
        BigDecimal tare = either(request.getEmptyVehicleKg(), request.getTareKg(), "emptyVehicleKg");
        return new DispatchMeasurement(
                gross, tare, request.getBagCount(), request.getScaleTicketNo(), request.getShortfallReason());
    }

    /** Eş anlamlı iki alandan biri zorunludur; ikisi de verilirse eşit olmalıdır. */
    private static BigDecimal either(@Nullable String preferred, @Nullable String alias, String field) {
        if (preferred == null && alias == null) {
            throw new InvalidDispatchException(field, "required");
        }
        if (preferred != null && alias != null && new BigDecimal(preferred).compareTo(new BigDecimal(alias)) != 0) {
            throw new InvalidDispatchException(field, "differs from alias");
        }
        return new BigDecimal(preferred != null ? preferred : alias);
    }
}
