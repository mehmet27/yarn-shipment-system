package com.yarnshipment.audit.application.port.in;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppendAuditEntryCommandTest {

    private static final UUID EVENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000000a1");
    private static final Instant AT = Instant.parse("2026-11-03T08:00:00Z");

    @Test
    @DisplayName("Kullanıcı ya da servis hesabından biri olmadan kayıt oluşmaz")
    void shouldRequireUserOrClient() {
        assertThatThrownBy(() -> command(new AppendAuditEntryCommand.Actor(null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(EVENT.toString());
        assertThatCode(() -> command(new AppendAuditEntryCommand.Actor(UUID.randomUUID(), null, null)))
                .doesNotThrowAnyException();
        assertThatCode(() -> command(new AppendAuditEntryCommand.Actor(null, null, "shipment-service")))
                .doesNotThrowAnyException();
    }

    private static AppendAuditEntryCommand command(AppendAuditEntryCommand.Actor actor) {
        return new AppendAuditEntryCommand(EVENT, AT, "shipment-service", actor, "type", null, null, null, null);
    }
}
