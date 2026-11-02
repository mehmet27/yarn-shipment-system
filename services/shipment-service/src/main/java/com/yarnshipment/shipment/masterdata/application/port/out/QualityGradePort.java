package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.domain.catalog.QualityGrade;
import com.yarnshipment.shipment.masterdata.domain.catalog.QualityGradeDetails;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.util.Optional;
import java.util.UUID;

/**
 * Kalite sınıfı okuma ve yazma portu. Liste koda göre sıralıdır, arama kod ve adda yapılır. Yazmalar değişikliği yapan
 * kullanıcıyı transaction'a işler (audit); kullanılmış kod {@code DuplicateCodeException} ile reddedilir.
 */
public interface QualityGradePort {

    MasterDataPage<QualityGrade> findQualityGrades(MasterDataQuery query);

    QualityGrade insertQualityGrade(UUID actorId, String code, QualityGradeDetails details);

    /** Güncellenen kaydı döner; kayıt yoksa boş. */
    Optional<QualityGrade> updateQualityGrade(UUID actorId, UUID id, QualityGradeDetails details);

    /** Kayıt yoksa {@code false}. */
    boolean setQualityGradeStatus(UUID actorId, UUID id, MasterDataStatus status);
}
