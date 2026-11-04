package com.yarnshipment.shipment.masterdata.application.port.in.catalog;

import com.yarnshipment.shipment.masterdata.domain.catalog.TareStandard;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Standart dara listesi; lokasyon verilirse yalnızca onunkiler. Lokasyon adına, sonra ambalaj tipine göre sıralı. */
public interface ListTareStandardsUseCase {

    List<TareStandard> listTareStandards(@Nullable UUID siteId);
}
