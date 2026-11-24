package com.yarnshipment.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.events.auth.AccountLocked;
import com.yarnshipment.events.auth.LoginFailed;
import com.yarnshipment.events.auth.LoginSucceeded;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class AuthEventsTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final Instant AT = Instant.parse("2026-11-24T09:00:00Z");

    @Test
    @DisplayName("Giriş olayları JSON'a yazılıp aynen okunur; türleri auth önekini taşır")
    void shouldRoundTripLoginEvents() {
        LoginSucceeded succeeded = new LoginSucceeded(USER, "depo", AT);
        LoginFailed failed = new LoginFailed(USER, "depo", AT);
        AccountLocked locked = new AccountLocked(USER, "depo", AT.plusSeconds(1), AT);

        assertThat(JSON.readValue(JSON.writeValueAsString(succeeded), LoginSucceeded.class))
                .isEqualTo(succeeded);
        assertThat(JSON.readValue(JSON.writeValueAsString(failed), LoginFailed.class))
                .isEqualTo(failed);
        assertThat(JSON.readValue(JSON.writeValueAsString(locked), AccountLocked.class))
                .isEqualTo(locked);
        assertThat(LoginSucceeded.TYPE).startsWith("com.yarnshipment.auth.");
        assertThat(LoginFailed.TYPE).startsWith("com.yarnshipment.auth.");
        assertThat(AccountLocked.TYPE).startsWith("com.yarnshipment.auth.");
    }
}
