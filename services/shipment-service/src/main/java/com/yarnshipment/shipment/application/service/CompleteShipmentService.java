package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.CompleteShipmentCommand;
import com.yarnshipment.shipment.application.port.in.CompleteShipmentUseCase;
import com.yarnshipment.shipment.application.port.out.CompletionPort;
import com.yarnshipment.shipment.application.port.out.EvidenceLookupPort;
import com.yarnshipment.shipment.domain.completion.Completion;
import com.yarnshipment.shipment.domain.evidence.InvalidWaybillEvidenceException;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * İrsaliye müdüre ulaşınca sevkiyatı tamamlar (saha analizi, 10.11.2026). Aynı irsaliye numarasıyla tamamlanmış
 * sevkiyata gelen ikinci istek yeni kayıt üretmez, başarılı sayılır (API-04). İrsaliye belgesi önce evidence-service'e
 * yüklenir; burada belgenin bu sevkiyata ait geçerli bir irsaliye olduğu denetlenir.
 */
@Service
public class CompleteShipmentService implements CompleteShipmentUseCase {

    private final ScopedShipmentLoader shipments;
    private final CompletionPort completionPort;
    private final EvidenceLookupPort evidence;
    private final Clock clock;

    public CompleteShipmentService(
            ScopedShipmentLoader shipments, CompletionPort completionPort, EvidenceLookupPort evidence, Clock clock) {
        this.shipments = shipments;
        this.completionPort = completionPort;
        this.evidence = evidence;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void completeShipment(CompleteShipmentCommand command) {
        Actor actor = command.actor();
        Shipment shipment = shipments.load(actor, command.shipmentId(), Permission.SHIPMENT_COMPLETE);
        if (isRepeatedRequest(shipment, command.waybillNo())) {
            return;
        }
        Completion completion =
                Completion.of(shipment, command.waybillNo(), command.waybillEvidenceId(), clock.instant());
        requireWaybillDocument(shipment.id(), command.waybillEvidenceId());
        completionPort.complete(completion, actor.userId());
    }

    private void requireWaybillDocument(UUID shipmentId, UUID evidenceId) {
        boolean valid = evidence.findEvidence(evidenceId)
                .filter(found -> found.isWaybillOf(shipmentId))
                .isPresent();
        if (!valid) {
            throw new InvalidWaybillEvidenceException(shipmentId, evidenceId);
        }
    }

    private static boolean isRepeatedRequest(Shipment shipment, String waybillNo) {
        return shipment.status() == ShipmentStatus.COMPLETED
                && waybillNo.strip().equals(shipment.waybillNo());
    }
}
