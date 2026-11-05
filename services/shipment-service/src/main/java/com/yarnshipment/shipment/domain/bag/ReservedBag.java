package com.yarnshipment.shipment.domain.bag;

import com.yarnshipment.shipment.domain.model.DispatchBag.PackageType;
import java.util.UUID;

/** Çuval servisinin sevkiyata ayırdığı çuval; sevkiyat çuvalı kopya olarak saklar, FK yoktur. */
public record ReservedBag(
        UUID bagId, String uniqueKey, PackageType packageType, String lotNo, String qualityGradeCode) {}
