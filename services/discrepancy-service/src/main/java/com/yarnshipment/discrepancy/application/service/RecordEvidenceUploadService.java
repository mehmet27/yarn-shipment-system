package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.discrepancy.application.port.in.ListDiscrepanciesQuery;
import com.yarnshipment.discrepancy.application.port.in.RecordEvidenceUploadCommand;
import com.yarnshipment.discrepancy.application.port.in.RecordEvidenceUploadUseCase;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyDecisionPort;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyQueryPort;
import com.yarnshipment.discrepancy.application.port.out.ShipmentEvidencePort;
import com.yarnshipment.discrepancy.domain.model.ComparisonRule;
import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import com.yarnshipment.discrepancy.domain.model.Resolution;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Yüklenen kanıtın türünü sevkiyata yazar (zorunlu fotoğraf kontrolü için) ve kanıt sonradan geldiyse eksikliği için
 * açılmış uyuşmazlığı sistem hesabıyla {@code DATA_ERROR} kararıyla kapatır: eksik olan veri sonradan geldi. İmzalı
 * teslim belgesi {@code DOCUMENT_MISSING}'i, zorunlu fotoğraf aynı türdeki {@code PHOTO_MISSING}'i kapatır. Diğer
 * kurallar (irsaliye alanlarının tutmaması) insan kararı ister ve açık kalır. Olay tekrar gelirse kapatılacak
 * uyuşmazlık kalmadığı için hiçbir şey yazılmaz. Karar olayı ({@code DiscrepancyResolved}) son açık uyuşmazlıkta
 * sevkiyatı tamamlatır.
 */
@Service
public class RecordEvidenceUploadService implements RecordEvidenceUploadUseCase {

    /** evidence-service'teki imzalı teslim belgesi (irsaliye) kanıt türü. */
    private static final String SIGNED_DELIVERY_DOC = "SIGNED_DELIVERY_DOC";

    private static final int MAX_PER_SHIPMENT = 100;

    private final DiscrepancyQueryPort queryPort;
    private final DiscrepancyDecisionPort decisionPort;
    private final ShipmentEvidencePort evidencePort;
    private final Clock clock;

    public RecordEvidenceUploadService(
            DiscrepancyQueryPort queryPort,
            DiscrepancyDecisionPort decisionPort,
            ShipmentEvidencePort evidencePort,
            Clock clock) {
        this.queryPort = queryPort;
        this.decisionPort = decisionPort;
        this.evidencePort = evidencePort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int recordEvidenceUpload(RecordEvidenceUploadCommand command) {
        evidencePort.recordUploaded(
                command.evidenceId(), command.shipmentId(), command.evidenceType(), command.uploadedAt());
        ListDiscrepanciesQuery.Filter ofShipment =
                new ListDiscrepanciesQuery.Filter(null, null, null, command.shipmentId(), null);
        List<Discrepancy> missing =
                queryPort.findDiscrepancies(ofShipment, AccessScope.UNRESTRICTED, 0, MAX_PER_SHIPMENT).items().stream()
                        .filter(discrepancy -> discrepancy.status().isUnresolved())
                        .filter(closedBy(command.evidenceType()))
                        .toList();
        Instant at = clock.instant();
        for (Discrepancy discrepancy : missing) {
            Discrepancy resolved = discrepancy.resolve(new Discrepancy.Decision(
                    Resolution.DATA_ERROR, noteFor(command), null, SystemDefaults.SYSTEM_ACTOR, at));
            decisionPort.saveResolution(resolved, SystemDefaults.SYSTEM_ACTOR, at);
        }
        return missing.size();
    }

    /** Bu kanıt türünün eksikliği için açılmış uyuşmazlık. */
    private static Predicate<Discrepancy> closedBy(String evidenceType) {
        if (SIGNED_DELIVERY_DOC.equals(evidenceType)) {
            return discrepancy -> ComparisonRule.DOCUMENT_MISSING
                    .name()
                    .equals(discrepancy.basis().ruleCode());
        }
        return discrepancy ->
                ComparisonRule.PHOTO_MISSING.name().equals(discrepancy.basis().ruleCode())
                        && evidenceType.equals(discrepancy.measure().expected());
    }

    private static String noteFor(RecordEvidenceUploadCommand command) {
        if (SIGNED_DELIVERY_DOC.equals(command.evidenceType())) {
            return "İmzalı teslim belgesi sonradan yüklendi (kanıt %s).".formatted(command.evidenceId());
        }
        return "Zorunlu fotoğraf sonradan yüklendi (kanıt %s).".formatted(command.evidenceId());
    }
}
