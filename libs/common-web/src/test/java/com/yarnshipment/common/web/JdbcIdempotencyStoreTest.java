package com.yarnshipment.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.web.idempotency.Claim;
import com.yarnshipment.common.web.idempotency.IdempotencyProperties;
import com.yarnshipment.common.web.idempotency.JdbcIdempotencyStore;
import com.yarnshipment.common.web.idempotency.StoredResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Gerçek PostgreSQL'e karşı: tek INSERT ile sahiplenme, tekrar, çakışma ve süre aşımı. */
class JdbcIdempotencyStoreTest {

    private static final Instant START = Instant.parse("2026-10-21T10:00:00Z");
    private static final String ACTOR = "depo";
    private static final String KEY = "key-1";
    private static final String FINGERPRINT = "abc";
    private static final int CREATED = 201;
    private static final Duration RETENTION = Duration.ofHours(24);
    private static final Duration IN_PROGRESS_TIMEOUT = Duration.ofMinutes(2);

    private final MutableClock clock = new MutableClock(START);
    private final JdbcIdempotencyStore store = new JdbcIdempotencyStore(
            PostgresTestDatabase.jdbc(), clock, new IdempotencyProperties(RETENTION, IN_PROGRESS_TIMEOUT));

    @BeforeEach
    void clean() {
        PostgresTestDatabase.jdbc().sql("DELETE FROM idempotency_record").update();
    }

    private static StoredResponse created() {
        return new StoredResponse(
                CREATED, "application/json", "/things/1", "{\"id\":1}".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("İlk istek anahtarı sahiplenir, ikincisi devam ediyor görür")
    void shouldClaimOnceAndReportInProgress() {
        assertThat(store.claim(ACTOR, KEY, FINGERPRINT)).isInstanceOf(Claim.Acquired.class);
        assertThat(store.claim(ACTOR, KEY, FINGERPRINT)).isInstanceOf(Claim.InProgress.class);
    }

    @Test
    @DisplayName("Tamamlanan anahtar aynı içerikle ilk yanıtı, farklı içerikle çakışmayı döner")
    void shouldReplayCompletedResponseAndDetectMismatch() {
        store.claim(ACTOR, KEY, FINGERPRINT);
        store.complete(ACTOR, KEY, created());

        Claim same = store.claim(ACTOR, KEY, FINGERPRINT);

        assertThat(same).isInstanceOf(Claim.Completed.class);
        StoredResponse replay = ((Claim.Completed) same).response();
        assertThat(replay.status()).isEqualTo(CREATED);
        assertThat(replay.contentType()).isEqualTo("application/json");
        assertThat(replay.location()).isEqualTo("/things/1");
        assertThat(new String(replay.body(), StandardCharsets.UTF_8)).isEqualTo("{\"id\":1}");
        assertThat(store.claim(ACTOR, KEY, "other")).isInstanceOf(Claim.Mismatch.class);
    }

    @Test
    @DisplayName("Anahtar kullanıcıya özeldir")
    void shouldScopeKeyToActor() {
        store.claim(ACTOR, KEY, FINGERPRINT);

        assertThat(store.claim("sofor", KEY, FINGERPRINT)).isInstanceOf(Claim.Acquired.class);
    }

    @Test
    @DisplayName("Bırakılan anahtar yeniden sahiplenilebilir")
    void shouldAllowReclaimAfterRelease() {
        store.claim(ACTOR, KEY, FINGERPRINT);
        store.release(ACTOR, KEY);

        assertThat(store.claim(ACTOR, KEY, FINGERPRINT)).isInstanceOf(Claim.Acquired.class);
    }

    @Test
    @DisplayName("Çöken isteğin açık bıraktığı anahtar zaman aşımından sonra serbest kalır")
    void shouldExpireStaleInProgressClaim() {
        store.claim(ACTOR, KEY, FINGERPRINT);
        clock.advance(IN_PROGRESS_TIMEOUT.plusMinutes(1));

        assertThat(store.claim(ACTOR, KEY, FINGERPRINT)).isInstanceOf(Claim.Acquired.class);
    }

    @Test
    @DisplayName("Saklama süresi dolan tamamlanmış kayıt yeni istek gibi işlenir")
    void shouldExpireCompletedRecordAfterRetention() {
        store.claim(ACTOR, KEY, FINGERPRINT);
        store.complete(ACTOR, KEY, created());
        clock.advance(RETENTION.plusHours(1));

        assertThat(store.claim(ACTOR, KEY, FINGERPRINT)).isInstanceOf(Claim.Acquired.class);
    }
}
