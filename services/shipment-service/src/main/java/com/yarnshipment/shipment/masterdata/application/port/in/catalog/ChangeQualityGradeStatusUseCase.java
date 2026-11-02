package com.yarnshipment.shipment.masterdata.application.port.in.catalog;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.util.UUID;

/** Ana veri yönetimi: ChangeQualityGradeStatus. Yalnızca MASTER_DATA_MANAGE. */
public interface ChangeQualityGradeStatusUseCase {

    void changeQualityGradeStatus(Actor actor, UUID id, MasterDataStatus status);
}
