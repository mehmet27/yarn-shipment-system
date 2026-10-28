package com.yarnshipment.evidence.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.UUID;

/** Kanıt üst verisi sorgusu. */
public record GetEvidenceQuery(Actor actor, UUID evidenceId) {}
