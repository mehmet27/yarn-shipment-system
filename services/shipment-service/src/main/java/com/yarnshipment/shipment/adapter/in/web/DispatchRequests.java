package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.shipment.adapter.in.web.api.model.DispatchWeighingRequest;
import com.yarnshipment.shipment.domain.model.DispatchMeasurement;
import java.math.BigDecimal;

/** Çıkış tartımı isteğini domain değerlerine çevirir. Kilo dizeleri sözleşmedeki desenle zaten doğrulanmıştır. */
final class DispatchRequests {

    private DispatchRequests() {}

    static DispatchMeasurement toMeasurement(DispatchWeighingRequest request) {
        return new DispatchMeasurement(
                new BigDecimal(request.getGrossKg()),
                new BigDecimal(request.getTareKg()),
                request.getBagCount(),
                request.getScaleTicketNo(),
                request.getSealNo());
    }
}
