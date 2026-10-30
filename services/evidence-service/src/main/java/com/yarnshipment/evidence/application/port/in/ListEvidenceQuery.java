package com.yarnshipment.evidence.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.UUID;

/** Bir sevkiyatın kanıtlarını listeleme sorgusu. */
public record ListEvidenceQuery(Actor actor, UUID shipmentId) {}
