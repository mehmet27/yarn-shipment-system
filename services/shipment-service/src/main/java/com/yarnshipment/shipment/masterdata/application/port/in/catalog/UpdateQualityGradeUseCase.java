package com.yarnshipment.shipment.masterdata.application.port.in.catalog;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.catalog.QualityGrade;
import com.yarnshipment.shipment.masterdata.domain.catalog.QualityGradeDetails;
import java.util.UUID;

/** Ana veri yönetimi: UpdateQualityGrade. Kod değişmez; yalnızca MASTER_DATA_MANAGE. */
public interface UpdateQualityGradeUseCase {

    QualityGrade updateQualityGrade(Actor actor, UUID id, QualityGradeDetails details);
}
