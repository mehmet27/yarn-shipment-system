package com.yarnshipment.evidence.application.port.in;

import com.yarnshipment.evidence.domain.model.EvidenceFile;

/** Kanıt üst verisini okur; sevkiyatı isteyene görünmüyorsa kanıt da yoktur. */
public interface GetEvidenceUseCase {

    EvidenceFile getEvidence(GetEvidenceQuery query);
}
