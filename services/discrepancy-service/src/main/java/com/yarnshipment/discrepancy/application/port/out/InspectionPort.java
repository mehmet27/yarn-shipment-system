package com.yarnshipment.discrepancy.application.port.out;

import com.yarnshipment.discrepancy.domain.claim.Inspection;
import com.yarnshipment.discrepancy.domain.claim.InspectionKind;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** İtiraz kontrollerinin (numune, lab) okuma ve yazımları; her yazım uyuşmazlık geçmişine de kayıt düşer. */
public interface InspectionPort {

    /** Uyuşmazlığın kontrolleri, istek sırasıyla. */
    List<Inspection> findInspections(UUID discrepancyId);

    Optional<Inspection> findInspection(UUID inspectionId);

    /** Kontrol isteğini yazar ve kimliğiyle döner. */
    Inspection saveRequest(NewInspection inspection);

    /** Sonucu yazar. Bu arada başkası sonuç girdiyse {@code ClaimRejectedException} fırlatır. */
    void saveResult(Inspection recorded);

    /**
     * Yazılacak kontrol isteği.
     *
     * @param shipmentId sevkiyat
     * @param discrepancyId itirazın uyuşmazlığı
     * @param kind numune ya da lab
     * @param request not, isteyen ve zaman
     */
    record NewInspection(UUID shipmentId, UUID discrepancyId, InspectionKind kind, Inspection.Request request) {}
}
