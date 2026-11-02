package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.shipment.adapter.in.web.api.model.CorrectionTarget;
import com.yarnshipment.shipment.adapter.in.web.api.model.CorrectionView;
import com.yarnshipment.shipment.adapter.in.web.api.model.RecordCorrectionRequest;
import com.yarnshipment.shipment.application.port.in.correction.ShipmentCorrection;
import com.yarnshipment.shipment.domain.correction.CorrectedValue;
import com.yarnshipment.shipment.domain.correction.Correction;
import com.yarnshipment.shipment.domain.correction.InvalidCorrectionException;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/** Düzeltme isteğinin alan modeline, kaydın görünüme çevrimi. */
final class CorrectionViews {

    private CorrectionViews() {}

    /** Tartımda brüt ve dara, mühürde numara zorunludur; eksik alan {@code INVALID_CORRECTION} olur. */
    static Correction.Request toRequest(RecordCorrectionRequest request) {
        var target = com.yarnshipment.shipment.domain.correction.CorrectionTarget.valueOf(
                request.getTarget().getValue());
        CorrectedValue value = target.isWeighing()
                ? new CorrectedValue.Weight(kg(request.getGrossKg(), "grossKg"), kg(request.getTareKg(), "tareKg"))
                : new CorrectedValue.SealNo(required(request.getSealNo(), "sealNo"));
        return new Correction.Request(target, value, request.getReason());
    }

    static CorrectionView toView(ShipmentCorrection correction) {
        return new CorrectionView(
                        correction.id(),
                        CorrectionTarget.fromValue(correction.target().name()),
                        correction.reason(),
                        CorrectionView.StatusEnum.fromValue(correction.status()),
                        correction.requestedBy(),
                        WebMappings.utc(correction.createdAt()))
                .oldValue(correction.oldValue())
                .newValue(correction.newValue())
                .approvedBy(correction.approvedBy());
    }

    private static BigDecimal kg(@Nullable String value, String field) {
        return new BigDecimal(required(value, field));
    }

    private static String required(@Nullable String value, String field) {
        if (value == null) {
            throw new InvalidCorrectionException(field);
        }
        return value;
    }
}
