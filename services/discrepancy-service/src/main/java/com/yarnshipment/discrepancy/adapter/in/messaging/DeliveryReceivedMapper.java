package com.yarnshipment.discrepancy.adapter.in.messaging;

import com.yarnshipment.discrepancy.domain.model.ComparisonInput;
import com.yarnshipment.discrepancy.domain.model.DeliveryFigures;
import com.yarnshipment.discrepancy.domain.model.DispatchFigures;
import com.yarnshipment.discrepancy.domain.model.DocumentFigures;
import com.yarnshipment.discrepancy.domain.model.SealCondition;
import com.yarnshipment.discrepancy.domain.ticket.ScaleWeights;
import com.yarnshipment.discrepancy.domain.ticket.WeighingEntries;
import com.yarnshipment.events.shipment.DeliveryReceived;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/** Olay sözleşmesini karşılaştırma girdisine çevirir. */
final class DeliveryReceivedMapper {

    private DeliveryReceivedMapper() {}

    /** Bağlamı (lokasyon) taşımayan eski olaylar işlenemez; kalıcı hatadır, DLT'ye gider (EVT-09). */
    static ComparisonInput toInput(DeliveryReceived event) {
        DeliveryReceived.Context context = event.context();
        if (context == null) {
            throw new IllegalArgumentException("DeliveryReceived without context shipmentId=" + event.shipmentId());
        }
        DeliveryReceived.Weighing dispatch = event.dispatch();
        return new ComparisonInput(
                event.shipmentId(),
                context.siteId(),
                context.shipmentNo(),
                event.submittedAt(),
                new DispatchFigures(
                        dispatch == null ? null : dispatch.netKg(),
                        dispatch == null ? null : dispatch.bagCount(),
                        event.dispatchSeal().sealNo()),
                new DeliveryFigures(
                        event.delivery().netKg(),
                        event.delivery().bagCount(),
                        event.deliverySeal().sealNo(),
                        sealCondition(event.deliverySeal().condition())),
                event.expectedUniqueKeys(),
                event.scannedUniqueKeys(),
                document(context.document()),
                context.subcontractorId());
    }

    /** Kantar fişleriyle karşılaştırılacak elle girişler; plakayı taşımayan eski olayda plaka kontrolü atlanır. */
    static WeighingEntries toEntries(DeliveryReceived event) {
        DeliveryReceived.Weighing dispatch = event.dispatch();
        DeliveryReceived.Context context = event.context();
        return new WeighingEntries(
                dispatch == null ? null : weights(dispatch),
                weights(event.delivery()),
                context == null ? null : context.plateNo());
    }

    private static ScaleWeights weights(DeliveryReceived.Weighing weighing) {
        return new ScaleWeights(weighing.grossKg(), weighing.tareKg(), weighing.netKg());
    }

    private static SealCondition sealCondition(@Nullable String condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Delivery seal condition is missing");
        }
        return SealCondition.valueOf(condition.toUpperCase(Locale.ROOT));
    }

    private static DocumentFigures document(DeliveryReceived.@Nullable Document document) {
        if (document == null) {
            return DocumentFigures.NONE;
        }
        return new DocumentFigures(
                document.shipmentNo(), document.netKg(), document.bagCount(), document.signedDocumentEvidenceId());
    }
}
