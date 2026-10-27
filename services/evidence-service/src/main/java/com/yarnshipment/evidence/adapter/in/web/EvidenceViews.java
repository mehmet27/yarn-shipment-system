package com.yarnshipment.evidence.adapter.in.web;

import com.yarnshipment.evidence.adapter.in.web.api.model.EvidenceSource;
import com.yarnshipment.evidence.adapter.in.web.api.model.EvidenceType;
import com.yarnshipment.evidence.adapter.in.web.api.model.EvidenceView;
import com.yarnshipment.evidence.adapter.in.web.api.model.RequestUploadRequest;
import com.yarnshipment.evidence.adapter.in.web.api.model.UploadTicketView;
import com.yarnshipment.evidence.application.port.in.UploadTicket;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import com.yarnshipment.evidence.domain.model.UploadIntent.DeclaredFile;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Alan modeli ile üretilen API modelleri arasındaki dönüşümler. */
final class EvidenceViews {

    private EvidenceViews() {}

    static DeclaredFile declaredOf(RequestUploadRequest request) {
        OffsetDateTime capturedAt = request.getCapturedAt();
        return new DeclaredFile(
                com.yarnshipment.evidence.domain.model.EvidenceType.valueOf(
                        request.getEvidenceType().getValue()),
                request.getMimeType(),
                request.getSizeBytes(),
                com.yarnshipment.evidence.domain.model.EvidenceSource.valueOf(
                        request.getSource().getValue()),
                capturedAt == null ? null : capturedAt.toInstant());
    }

    static UploadTicketView toView(UploadTicket ticket) {
        return new UploadTicketView(
                ticket.evidenceId(),
                ticket.uploadUrl(),
                ticket.headers(),
                ticket.expiresAt().atOffset(ZoneOffset.UTC));
    }

    static EvidenceView toView(EvidenceFile file) {
        EvidenceView view = new EvidenceView(
                file.id(),
                file.shipmentId(),
                EvidenceType.fromValue(file.declared().type().name()),
                file.stored().mimeType(),
                file.stored().sizeBytes(),
                file.stored().sha256(),
                EvidenceSource.fromValue(file.declared().source().name()),
                file.uploadedAt().atOffset(ZoneOffset.UTC));
        if (file.declared().capturedAt() != null) {
            view.setCapturedAt(file.declared().capturedAt().atOffset(ZoneOffset.UTC));
        }
        return view;
    }
}
