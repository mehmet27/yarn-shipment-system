package com.yarnshipment.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.web.WebTestApp.TestController;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class IdempotencyFilterTest {

    private static final int TOO_LONG = 256;
    private static final String KEY = "0192f0a0-7000-4000-8000-000000000001";

    private final FakeIdempotencyStore store = new FakeIdempotencyStore();
    private final TestController controller = new TestController();
    private final MockMvcTester mvc = MockMvcTester.create(WebTestApp.mockMvc(store, controller));

    @BeforeEach
    void authenticate() {
        authenticateAs("depo");
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticateAs(String name) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(name, "n/a", "ROLE_X"));
    }

    private MvcTestResult post(@Nullable String key, String body) {
        return mvc.post()
                .uri("/things")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    if (key != null) {
                        headers.add("Idempotency-Key", key);
                    }
                })
                .content(body)
                .exchange();
    }

    @Test
    @DisplayName("Aynı anahtar ve aynı gövde ilk yanıtı tekrar döner, istek bir kez çalışır")
    void shouldReplayFirstResponseForSameKeyAndBody() {
        MvcTestResult first = post(KEY, "{\"a\":1}");
        MvcTestResult second = post(KEY, "{\"a\":1}");

        assertThat(first).hasStatus(HttpStatus.CREATED);
        assertThat(second).hasStatus(HttpStatus.CREATED);
        assertThat(second)
                .bodyText()
                .isEqualTo(new String(first.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8));
        assertThat(second.getResponse().getHeader("Idempotency-Replayed")).isEqualTo("true");
        assertThat(second.getResponse().getHeader("Location")).isEqualTo("/things/1");
        assertThat(first.getResponse().getHeader("Idempotency-Replayed")).isNull();
        assertThat(controller.calls).hasValue(1);
    }

    @Test
    @DisplayName("Aynı anahtar farklı gövdeyle gelirse 422 ve IDEMPOTENCY_KEY_REUSED döner")
    void shouldRejectSameKeyWithDifferentBody() {
        post(KEY, "{\"a\":1}");
        MvcTestResult second = post(KEY, "{\"a\":2}");

        assertThat(second).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(second).bodyJson().extractingPath("$.code").isEqualTo("IDEMPOTENCY_KEY_REUSED");
        assertThat(controller.calls).hasValue(1);
    }

    @Test
    @DisplayName("Başarısız istek anahtarı bırakır; aynı anahtarla yeniden denenebilir")
    void shouldReleaseKeyWhenRequestFails() {
        MvcTestResult failed = post(KEY, "boom");

        assertThat(failed).hasStatus(HttpStatus.CONFLICT);
        assertThat(store.size()).isZero();
        assertThat(post(KEY, "boom")).hasStatus(HttpStatus.CONFLICT);
        assertThat(controller.calls).hasValue(2);
    }

    @Test
    @DisplayName("Anahtar kullanıcıya özeldir; başka kullanıcının aynı anahtarı ayrı işlem sayılır")
    void shouldScopeKeyToActor() {
        post(KEY, "{\"a\":1}");
        authenticateAs("sofor");

        assertThat(post(KEY, "{\"a\":1}")).hasStatus(HttpStatus.CREATED);
        assertThat(controller.calls).hasValue(2);
    }

    @Test
    @DisplayName("Devam eden aynı anahtarlı istek 409 ve IDEMPOTENCY_IN_PROGRESS alır")
    void shouldReportInProgressRequest() {
        store.holdOpen();
        post(KEY, "{\"a\":1}");

        MvcTestResult result = post(KEY, "{\"a\":1}");

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("IDEMPOTENCY_IN_PROGRESS");
        assertThat(controller.calls).hasValue(1);
    }

    @Test
    @DisplayName("Anahtarsız istek olduğu gibi geçer")
    void shouldPassThroughWithoutKey() {
        assertThat(post(null, "{\"a\":1}")).hasStatus(HttpStatus.CREATED);
        assertThat(post(null, "{\"a\":1}")).hasStatus(HttpStatus.CREATED);
        assertThat(controller.calls).hasValue(2);
    }

    @Test
    @DisplayName("Boşluklu veya çok uzun anahtar 400 ve INVALID_IDEMPOTENCY_KEY alır")
    void shouldRejectInvalidKey() {
        MvcTestResult spaced = post("bad key", "{}");
        MvcTestResult tooLong = post("k".repeat(TOO_LONG), "{}");

        assertThat(spaced).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(spaced).bodyJson().extractingPath("$.code").isEqualTo("INVALID_IDEMPOTENCY_KEY");
        assertThat(tooLong).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(controller.calls).hasValue(0);
    }

    @Test
    @DisplayName("Oturumsuz istek anahtar olsa bile filtreden geçer")
    void shouldPassThroughWhenUnauthenticated() {
        SecurityContextHolder.clearContext();

        assertThat(post(KEY, "{}")).hasStatus(HttpStatus.CREATED);
        assertThat(store.size()).isZero();
    }
}
