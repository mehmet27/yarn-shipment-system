package com.yarnshipment.reporting.application.port.in;

import com.yarnshipment.reporting.domain.model.ReadModelChange;
import java.util.UUID;

/**
 * Okuma modeline uygulanacak değişiklik ve kaynağı olan olay.
 *
 * @param eventId idempotency anahtarı (EVT-05)
 * @param eventType olay türü
 * @param change değişiklik
 */
public record ApplyReadModelChangeCommand(UUID eventId, String eventType, ReadModelChange change) {}
