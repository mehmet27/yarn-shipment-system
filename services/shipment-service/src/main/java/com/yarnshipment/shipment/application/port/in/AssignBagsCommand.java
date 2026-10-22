package com.yarnshipment.shipment.application.port.in;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.domain.model.ScanMethod;
import java.util.List;
import java.util.UUID;

/** Çuvalları taslak sevkiyata atama isteği. */
public record AssignBagsCommand(Actor actor, UUID shipmentId, List<String> uniqueKeys, ScanMethod scanMethod) {}
