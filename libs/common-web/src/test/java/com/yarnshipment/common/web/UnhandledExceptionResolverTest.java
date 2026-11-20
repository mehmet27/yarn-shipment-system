package com.yarnshipment.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.web.WebTestApp.TestController;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@ExtendWith(OutputCaptureExtension.class)
class UnhandledExceptionResolverTest {

    private final MockMvc mockMvc = WebTestApp.mockMvc(new FakeIdempotencyStore(), new TestController());
    private final MockMvcTester mvc = MockMvcTester.create(mockMvc);

    @Test
    @DisplayName("Karşılanmayan istisna 500 INTERNAL_ERROR döner; istisna mesajı ve sınıf adı yanıta sızmaz")
    void shouldHideUnhandledExceptionDetails() {
        MvcTestResult result =
                mvc.get().uri("/things/broken").header("X-Request-Id", "req-9").exchange();

        assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result).hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("INTERNAL_ERROR");
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Beklenmeyen bir hata oluştu");
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result).bodyJson().extractingPath("$.instance").isEqualTo("/things/broken");
        assertThat(result).bodyJson().extractingPath("$.requestId").isEqualTo("req-9");
        assertThat(result).bodyText().doesNotContain("secret_table", "hunter2", "IllegalStateException");
    }

    @Test
    @DisplayName("Karşılanmayan istisna yığın izi ve istek yoluyla bir kez ERROR olarak loglanır")
    void shouldLogUnhandledExceptionOnce(CapturedOutput output) {
        mvc.get().uri("/things/broken").exchange();

        assertThat(output.getAll())
                .containsOnlyOnce("Unhandled exception method=GET path=/things/broken")
                .contains("java.lang.IllegalStateException")
                .contains("at com.yarnshipment.common.web.WebTestApp$TestController.broken");
    }

    @Test
    @DisplayName("Yetki istisnası 500'e çevrilmez; güvenlik zincirine ulaşsın diye olduğu gibi yükselir")
    void shouldLeaveAccessDeniedToSecurityChain() {
        assertThatThrownBy(() -> mockMvc.perform(MockMvcRequestBuilders.get("/things/denied")))
                .isInstanceOf(ServletException.class)
                .hasCauseInstanceOf(AccessDeniedException.class);
    }
}
