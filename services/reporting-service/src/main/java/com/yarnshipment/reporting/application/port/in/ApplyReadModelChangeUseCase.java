package com.yarnshipment.reporting.application.port.in;

/** Olaydan gelen değişikliği okuma modeline uygular; aynı olay ikinci kez uygulanmaz. */
public interface ApplyReadModelChangeUseCase {

    void applyChange(ApplyReadModelChangeCommand command);
}
