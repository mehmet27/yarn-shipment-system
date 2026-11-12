package com.yarnshipment.moduleapi;

import java.io.Serial;

/** Çağrılan modül yanıt veremedi ya da beklenmeyen sonuç döndü (ağ üzerinden çağrılırken ulaşılamama dahil). */
public final class ModuleUnavailableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ModuleUnavailableException(String message) {
        super(message);
    }

    public ModuleUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
