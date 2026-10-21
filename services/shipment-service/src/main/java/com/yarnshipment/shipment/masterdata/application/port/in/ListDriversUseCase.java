package com.yarnshipment.shipment.masterdata.application.port.in;

import com.yarnshipment.shipment.masterdata.domain.model.Driver;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Etkin şoförlerin sayfalı listesi; {@code subcontractorId} verilirse yalnızca o fasoncunun şoförleri. */
public interface ListDriversUseCase {

    MasterDataPage<Driver> listDrivers(MasterDataQuery query, @Nullable UUID subcontractorId);
}
