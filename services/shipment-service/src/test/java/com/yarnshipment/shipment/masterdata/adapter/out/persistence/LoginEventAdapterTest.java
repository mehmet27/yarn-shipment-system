package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.events.testing.RecordingDomainEventPublisher;
import com.yarnshipment.events.auth.AccountLocked;
import com.yarnshipment.events.auth.LoginFailed;
import com.yarnshipment.events.auth.LoginSucceeded;
import com.yarnshipment.moduleapi.event.DomainEvent;
import com.yarnshipment.shipment.masterdata.domain.model.AuthUser;
import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoginEventAdapterTest {

    private static final Instant NOW = Instant.parse("2026-11-24T09:00:00Z");
    private static final Instant LOCKED_UNTIL = Instant.parse("2026-11-24T09:15:00Z");
    private static final AuthUser USER = new AuthUser(
            UUID.fromString("0192f0a0-0000-7000-8000-00000000d001"),
            "depo",
            "Depo Görevlisi",
            StaffRole.WAREHOUSE,
            "hash",
            true,
            false,
            null,
            Set.of());

    private final RecordingDomainEventPublisher events = new RecordingDomainEventPublisher();
    private final LoginEventAdapter adapter = new LoginEventAdapter(events);

    @Test
    @DisplayName("Giriş olayları hesabın kimliğiyle yayınlanır; aktör hesabın kendisidir ve gövdede şifre yoktur")
    void shouldPublishLoginEventsForTheAccount() {
        adapter.publishSucceeded(USER, NOW);
        adapter.publishFailed(USER, NOW);
        adapter.publishLocked(USER, LOCKED_UNTIL, NOW);

        assertThat(events.published())
                .extracting(DomainEvent::type)
                .containsExactly(LoginSucceeded.TYPE, LoginFailed.TYPE, AccountLocked.TYPE);
        assertThat(events.published()).allSatisfy(event -> {
            assertThat(event.aggregateType()).isEqualTo("LoginAccount");
            assertThat(event.aggregateId()).isEqualTo(USER.id());
            assertThat(event.actorId()).isEqualTo(USER.id());
            assertThat(event.occurredAt()).isEqualTo(NOW);
            assertThat(event.payload().toString()).doesNotContain("hash");
        });
        assertThat(events.published().getLast().payload())
                .isEqualTo(new AccountLocked(USER.id(), "depo", LOCKED_UNTIL, NOW));
    }
}
