package com.yarnshipment.evidence.application.port.out;

import com.yarnshipment.evidence.domain.model.UploadIntent;
import java.util.Optional;
import java.util.UUID;

/** Onay bekleyen yüklemeler. */
public interface UploadIntentPort {

    void saveIntent(UploadIntent intent);

    Optional<UploadIntent> findIntent(UUID evidenceId);
}
