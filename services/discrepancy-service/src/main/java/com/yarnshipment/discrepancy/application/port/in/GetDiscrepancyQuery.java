package com.yarnshipment.discrepancy.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.UUID;

/** Tek uyuşmazlık sorgusu (detay ve geçmiş). */
public record GetDiscrepancyQuery(Actor actor, UUID discrepancyId) {}
