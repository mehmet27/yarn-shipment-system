package com.yarnshipment.evidence.application.port.in;

import com.yarnshipment.evidence.domain.model.EvidenceFile;
import java.util.List;

/** Sevkiyatın kanıtlarından isteyenin görmesine izin verilenleri döner (kör giriş dahil). */
public interface ListEvidenceUseCase {

    List<EvidenceFile> listEvidence(ListEvidenceQuery query);
}
