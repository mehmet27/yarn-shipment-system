package com.yarnshipment.discrepancy.adapter.in.web;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.DiscrepancyStatus;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.QualityClaimView;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.Resolution;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.ShipmentClaimsView;
import com.yarnshipment.discrepancy.application.port.in.claim.ClaimFile;
import com.yarnshipment.discrepancy.application.port.in.claim.ShipmentClaims;
import com.yarnshipment.discrepancy.domain.claim.QualityClaim;
import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/** İtiraz dosyasını web modeline çevirir; eylem listeleri kullanıcıya göre hesaplanır (FE-SEC-03). */
final class QualityClaimViews {

    private static final int KG_SCALE = 3;

    private QualityClaimViews() {}

    static ShipmentClaimsView toView(ShipmentClaims claims, Actor actor) {
        return new ShipmentClaimsView()
                .shipmentId(claims.target().shipmentId())
                .deadlineAt(utc(claims.deadline()))
                .windowOpen(claims.windowOpen())
                .receivedKg(kg(claims.target().receivedKg()))
                .claimableKg(kg(claims.target().claimableKg()))
                .claims(claims.claims().stream()
                        .map(file -> toView(file, actor))
                        .toList())
                .allowedActions(shipmentActions(claims, actor));
    }

    static QualityClaimView toView(ClaimFile file, Actor actor) {
        QualityClaim claim = file.claim();
        Discrepancy discrepancy = file.discrepancy();
        QualityClaimView view = new QualityClaimView()
                .id(claim.id())
                .discrepancyId(claim.discrepancyId())
                .discrepancyNo(discrepancy.discrepancyNo())
                .shipmentId(claim.shipmentId())
                .status(DiscrepancyStatus.fromValue(discrepancy.status().name()))
                .description(claim.description())
                .claimedKg(kg(claim.claimedKg()))
                .fabricReceiptId(claim.fabricReceiptId())
                .evidenceIds(claim.evidenceIds())
                .openedBy(claim.opening().openedBy())
                .openedAt(utc(claim.opening().openedAt()))
                .deadlineAt(utc(claim.opening().deadlineAt()));
        Discrepancy.Decision decision = discrepancy.decision();
        if (decision != null) {
            view.resolution(Resolution.fromValue(decision.resolution().name())).resolutionNote(decision.note());
        }
        return view;
    }

    /** Süre sürüyorsa ve kullanıcının izni varsa yeni itiraz açılabilir; kilo sınırı açılışta denetlenir. */
    private static List<ShipmentClaimsView.AllowedActionsEnum> shipmentActions(ShipmentClaims claims, Actor actor) {
        if (!claims.windowOpen() || !actor.hasPermission(Permission.QUALITY_CLAIM_OPEN)) {
            return List.of();
        }
        return List.of(ShipmentClaimsView.AllowedActionsEnum.OPEN_CLAIM);
    }

    private static String kg(BigDecimal value) {
        return value.setScale(KG_SCALE, RoundingMode.HALF_UP).toPlainString();
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
