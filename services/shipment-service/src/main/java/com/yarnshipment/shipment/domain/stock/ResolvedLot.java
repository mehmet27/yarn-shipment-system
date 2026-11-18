package com.yarnshipment.shipment.domain.stock;

import java.util.UUID;

/** Girişteki parti numarasının karşılığı olan parti. */
public record ResolvedLot(UUID id, String lotNo) {}
