package com.yarnshipment.discrepancy.application.port.in.claim;

import com.yarnshipment.common.security.Actor;
import java.util.UUID;

/** Sevkiyatın itiraz dosyası sorgusu. */
public record GetShipmentClaimsQuery(Actor actor, UUID shipmentId) {}
