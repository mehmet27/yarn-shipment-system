package com.yarnshipment.evidence.domain.ticket;

import java.util.Objects;

/**
 * Okumayı yapan model ve istem sürümü; okumayla birlikte saklanır.
 *
 * @param model model adı, ör. {@code claude-sonnet-5-5}
 * @param promptVersion istem ve şema sürümü
 */
public record ReaderVersion(String model, String promptVersion) {

    public ReaderVersion {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(promptVersion, "promptVersion");
    }
}
