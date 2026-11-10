package com.yarnshipment.evidence.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;

/**
 * Kanıt görüntüleme kuralı. Kör teslim girişi kalktığı için (saha analizi, 10.11.2026) kanıtı görme izni olan herkes
 * sevkiyatın bütün kanıtlarını görür; sahiplik ve lokasyon kapsamı ayrıca sevkiyat üzerinden denetlenir.
 */
final class EvidenceVisibility {

    private EvidenceVisibility() {}

    static boolean mayView(Actor actor) {
        return actor.hasPermission(Permission.EVIDENCE_VIEW);
    }
}
