package com.yarnshipment.shipment.domain.bag;

import com.yarnshipment.shipment.domain.model.ScanMethod;
import java.util.List;
import java.util.UUID;

/** Sevkiyata atanan çuvallar ve girişlerinin yöntemi. */
public record BagAssignment(UUID shipmentId, List<ReservedBag> bags, ScanMethod scanMethod) {}
