package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.ChangeQualityGradeStatusUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.CreateQualityGradeUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.ListQualityGradesUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.UpdateQualityGradeUseCase;
import com.yarnshipment.shipment.masterdata.application.port.out.QualityGradePort;
import com.yarnshipment.shipment.masterdata.domain.catalog.MasterDataCode;
import com.yarnshipment.shipment.masterdata.domain.catalog.QualityGrade;
import com.yarnshipment.shipment.masterdata.domain.catalog.QualityGradeDetails;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kalite sınıfı listesi, ekleme, güncelleme, pasife alma. Kod partinin kilidiyle çuvala kopyalandığı için eklendikten
 * sonra değişmez. Yazma yetkisi uçta verilir.
 */
@Service
public class QualityGradeManagementService
        implements ListQualityGradesUseCase,
                CreateQualityGradeUseCase,
                UpdateQualityGradeUseCase,
                ChangeQualityGradeStatusUseCase {

    private static final String KIND = "QualityGrade";

    private final QualityGradePort grades;

    public QualityGradeManagementService(QualityGradePort grades) {
        this.grades = grades;
    }

    @Override
    @Transactional(readOnly = true)
    public MasterDataPage<QualityGrade> listQualityGrades(MasterDataQuery query) {
        return grades.findQualityGrades(query);
    }

    @Override
    @Transactional
    public QualityGrade createQualityGrade(Actor actor, String code, QualityGradeDetails details) {
        return grades.insertQualityGrade(actor.userId(), MasterDataCode.normalize("code", code), details);
    }

    @Override
    @Transactional
    public QualityGrade updateQualityGrade(Actor actor, UUID id, QualityGradeDetails details) {
        return grades.updateQualityGrade(actor.userId(), id, details)
                .orElseThrow(() -> new MasterDataNotFoundException(KIND, id));
    }

    @Override
    @Transactional
    public void changeQualityGradeStatus(Actor actor, UUID id, MasterDataStatus status) {
        if (!grades.setQualityGradeStatus(actor.userId(), id, status)) {
            throw new MasterDataNotFoundException(KIND, id);
        }
    }
}
