package com.yarnshipment.evidence.application.port.in;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.evidence.domain.model.UploadIntent;
import java.util.UUID;

/**
 * Yükleme isteği.
 *
 * @param actor isteyen
 * @param shipmentId kanıtın ait olduğu sevkiyat
 * @param file bildirilen dosya özellikleri
 */
public record RequestUploadCommand(Actor actor, UUID shipmentId, UploadIntent.DeclaredFile file) {}
