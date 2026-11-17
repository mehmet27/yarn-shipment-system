package com.yarnshipment.shipment.masterdata.application.port.in;

import com.yarnshipment.shipment.masterdata.domain.model.Driver;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Etkin şoförlerin sayfalı listesi; {@code subcontractorId} verilirse o fasoncunun şoförleri ile firmanın kendi
 * (merkez) şoförleri birlikte gelir, başka fasoncuların şoförleri gelmez.
 */
public interface ListDriversUseCase {

    MasterDataPage<Driver> listDrivers(MasterDataQuery query, @Nullable UUID subcontractorId);
}
