package com.yarnshipment.discrepancy.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.UUID;

/** Uyuşmazlığı sorumluya atama komutu. */
public record AssignDiscrepancyCommand(Actor actor, UUID discrepancyId, UUID assigneeId) {}
