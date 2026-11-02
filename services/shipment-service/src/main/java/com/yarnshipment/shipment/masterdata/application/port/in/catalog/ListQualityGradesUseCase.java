package com.yarnshipment.shipment.masterdata.application.port.in.catalog;

import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.domain.catalog.QualityGrade;

/** Kalite sınıfı listesi; herkese açık referans veri. */
public interface ListQualityGradesUseCase {

    MasterDataPage<QualityGrade> listQualityGrades(MasterDataQuery query);
}
