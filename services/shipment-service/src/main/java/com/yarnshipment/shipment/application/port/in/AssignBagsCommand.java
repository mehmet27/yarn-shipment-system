package com.yarnshipment.shipment.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Çuvalları taslak sevkiyata ekleme isteği; her kilo bir çuvaldır. */
public record AssignBagsCommand(Actor actor, UUID shipmentId, List<BigDecimal> weightsKg) {}
