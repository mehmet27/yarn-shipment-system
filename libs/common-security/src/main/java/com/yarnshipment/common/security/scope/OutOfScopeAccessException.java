package com.yarnshipment.common.security.scope;

import java.io.Serial;
import org.springframework.security.access.AccessDeniedException;

/** Kaydın kapsamı aktörün kapsamı dışında (SEC-03). Güvenlik zinciri 403 ve problem cevabına çevirir. */
public final class OutOfScopeAccessException extends AccessDeniedException {

    @Serial
    private static final long serialVersionUID = 1L;

    public OutOfScopeAccessException(String resourceReference) {
        super("Resource is out of actor scope resource=" + resourceReference);
    }
}
