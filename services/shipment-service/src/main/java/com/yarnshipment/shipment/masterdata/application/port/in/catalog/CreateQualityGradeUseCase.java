package com.yarnshipment.shipment.masterdata.application.port.in.catalog;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.catalog.QualityGrade;
import com.yarnshipment.shipment.masterdata.domain.catalog.QualityGradeDetails;

/** Ana veri yönetimi: CreateQualityGrade. Yalnızca MASTER_DATA_MANAGE yetkisi olan yönetici çağırır. */
public interface CreateQualityGradeUseCase {

    QualityGrade createQualityGrade(Actor actor, String code, QualityGradeDetails details);
}
