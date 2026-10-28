package com.yarnshipment.evidence.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.Role;
import com.yarnshipment.evidence.domain.model.EvidenceType;

/**
 * Kanıt görüntüleme kuralı (yetkilendirme analizi, kör giriş): çıkış kantar fotoğrafı kiloyu gösterir, fasoncu onu
 * karşılaştırmadan önce göremez; depo da teslim fotoğraflarını göremez. Şoför yalnızca kendi plaka fotoğrafını görür.
 * Mühür fotoğrafı hangi aşamada çekildiği kayıtlı olmadığı için çıkış tarafı sayılır. Sahiplik ve lokasyon kapsamı
 * ayrıca sevkiyat üzerinden denetlenir.
 */
final class EvidenceVisibility {

    private enum Side {
        OUTBOUND,
        INBOUND,
        CLAIM
    }

    private EvidenceVisibility() {}

    static boolean mayView(Actor actor, EvidenceType type, boolean comparisonDone) {
        if (!actor.hasPermission(Permission.EVIDENCE_VIEW)) {
            return false;
        }
        Side side = sideOf(type);
        return seesEverything(actor)
                || warehouseMaySee(actor, side, comparisonDone)
                || subcontractorMaySee(actor, side, comparisonDone)
                || driverMaySee(actor, type);
    }

    private static boolean seesEverything(Actor actor) {
        return actor.hasAnyRole(Role.ADMIN, Role.QUALITY, Role.MANAGER, Role.SYSTEM);
    }

    private static boolean warehouseMaySee(Actor actor, Side side, boolean comparisonDone) {
        return actor.hasRole(Role.WAREHOUSE) && (side == Side.OUTBOUND || comparisonDone);
    }

    private static boolean subcontractorMaySee(Actor actor, Side side, boolean comparisonDone) {
        return actor.hasRole(Role.SUBCONTRACTOR) && (side != Side.OUTBOUND || comparisonDone);
    }

    private static boolean driverMaySee(Actor actor, EvidenceType type) {
        return actor.hasRole(Role.DRIVER) && type == EvidenceType.VEHICLE_PLATE;
    }

    private static Side sideOf(EvidenceType type) {
        return switch (type) {
            case DISPATCH_SCALE, BAG_OVERVIEW, BAG_LABEL, SEAL, VEHICLE_PLATE -> Side.OUTBOUND;
            case DELIVERY_SCALE, DELIVERY_BAGS, SIGNED_DELIVERY_DOC -> Side.INBOUND;
            case QUALITY_CLAIM, LAB_RESULT -> Side.CLAIM;
        };
    }
}
