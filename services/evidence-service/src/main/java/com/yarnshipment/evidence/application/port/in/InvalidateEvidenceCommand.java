package com.yarnshipment.evidence.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.UUID;

/**
 * Kanıtı geçersiz kılma komutu.
 *
 * @param actor isteyen
 * @param evidenceId kanıt
 * @param reason gerekçe; boş olamaz
 */
public record InvalidateEvidenceCommand(Actor actor, UUID evidenceId, String reason) {}
