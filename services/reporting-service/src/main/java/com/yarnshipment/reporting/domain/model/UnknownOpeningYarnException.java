package com.yarnshipment.reporting.domain.model;

import java.io.Serial;
import java.util.List;

/** Açılış dosyasında Tanımlar'da etkin olmayan iplik adı var; hiçbir satır yazılmaz. */
public final class UnknownOpeningYarnException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient List<String> yarnTypes;

    public UnknownOpeningYarnException(List<String> yarnTypes) {
        super("Unknown yarn types in opening import count=" + yarnTypes.size());
        this.yarnTypes = List.copyOf(yarnTypes);
    }

    public List<String> yarnTypes() {
        return yarnTypes;
    }
}
