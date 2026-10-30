package com.yarnshipment.discrepancy.adapter.in.web;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.DiscrepancyStatus;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.InspectionKind;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.InspectionView;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.QualityClaimView;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.Resolution;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.ShipmentClaimsView;
import com.yarnshipment.discrepancy.application.port.in.claim.ClaimFile;
import com.yarnshipment.discrepancy.application.port.in.claim.ShipmentClaims;
import com.yarnshipment.discrepancy.domain.claim.Inspection;
import com.yarnshipment.discrepancy.domain.claim.QualityClaim;
import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** İtiraz dosyasını web modeline çevirir; eylem listeleri kullanıcıya göre hesaplanır (FE-SEC-03). */
final class QualityClaimViews {

    private QualityClaimViews() {}

    static ShipmentClaimsView toView(ShipmentClaims claims, Actor actor) {
        return new ShipmentClaimsView()
                .shipmentId(claims.target().shipmentId())
                .deadlineAt(utc(claims.deadline()))
                .windowOpen(claims.windowOpen())
                .deliveredKeys(claims.target().deliveredKeys())
                .claims(claims.claims().stream()
                        .map(file -> toView(file, actor))
                        .toList())
                .allowedActions(shipmentActions(claims, actor));
    }

    static QualityClaimView toView(ClaimFile file, Actor actor) {
        QualityClaim claim = file.claim();
        Discrepancy discrepancy = file.discrepancy();
        boolean mayInspect = discrepancy.status().isUnresolved() && actor.hasPermission(Permission.DISCREPANCY_RESOLVE);
        QualityClaimView view = new QualityClaimView()
                .id(claim.id())
                .discrepancyId(claim.discrepancyId())
                .discrepancyNo(discrepancy.discrepancyNo())
                .shipmentId(claim.shipmentId())
                .status(DiscrepancyStatus.fromValue(discrepancy.status().name()))
                .description(claim.description())
                .bagKeys(claim.bagKeys())
                .evidenceIds(claim.evidenceIds())
                .openedBy(claim.opening().openedBy())
                .openedAt(utc(claim.opening().openedAt()))
                .deadlineAt(utc(claim.opening().deadlineAt()))
                .inspections(file.inspections().stream()
                        .map(inspection -> toView(inspection, mayInspect))
                        .toList())
                .allowedActions(
                        mayInspect ? List.of(QualityClaimView.AllowedActionsEnum.REQUEST_INSPECTION) : List.of());
        Discrepancy.Decision decision = discrepancy.decision();
        if (decision != null) {
            view.resolution(Resolution.fromValue(decision.resolution().name())).resolutionNote(decision.note());
        }
        return view;
    }

    /** Yeni istenen ya da sonucu girilen kontrol; itiraz bu noktada açık olduğu için eylem yalnızca izne bağlıdır. */
    static InspectionView toView(Inspection inspection, Actor actor) {
        return toView(inspection, actor.hasPermission(Permission.DISCREPANCY_RESOLVE));
    }

    private static InspectionView toView(Inspection inspection, boolean mayInspect) {
        InspectionView view = new InspectionView()
                .id(inspection.id())
                .kind(InspectionKind.fromValue(inspection.kind().name()))
                .requestNote(inspection.request().note())
                .requestedBy(inspection.request().requestedBy())
                .requestedAt(utc(inspection.request().requestedAt()))
                .allowedActions(
                        mayInspect && inspection.isPending()
                                ? List.of(InspectionView.AllowedActionsEnum.RECORD_RESULT)
                                : List.of());
        Inspection.Result result = inspection.result();
        if (result != null) {
            view.result(result.summary())
                    .resultEvidenceId(result.evidenceId())
                    .performedBy(result.performedBy())
                    .performedAt(utc(result.performedAt()));
        }
        return view;
    }

    /** Teslimdeki çuvalların hepsi itiraz edildiyse yeni itiraz açılamaz. */
    private static List<ShipmentClaimsView.AllowedActionsEnum> shipmentActions(ShipmentClaims claims, Actor actor) {
        Set<String> claimed = claims.claims().stream()
                .flatMap(file -> file.claim().bagKeys().stream())
                .collect(Collectors.toSet());
        boolean hasUnclaimed = claims.target().deliveredKeys().stream().anyMatch(key -> !claimed.contains(key));
        if (!claims.windowOpen() || !hasUnclaimed || !actor.hasPermission(Permission.QUALITY_CLAIM_OPEN)) {
            return List.of();
        }
        return List.of(ShipmentClaimsView.AllowedActionsEnum.OPEN_CLAIM);
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
