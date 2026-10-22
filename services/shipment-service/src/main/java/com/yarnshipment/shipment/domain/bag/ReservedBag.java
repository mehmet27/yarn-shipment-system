package com.yarnshipment.shipment.domain.bag;

import java.util.UUID;

/** Çuval servisinin sevkiyata ayırdığı çuval; sevkiyat çuvalı kopya olarak saklar, FK yoktur. */
public record ReservedBag(UUID bagId, String uniqueKey, String lotNo, String qualityGradeCode) {}
