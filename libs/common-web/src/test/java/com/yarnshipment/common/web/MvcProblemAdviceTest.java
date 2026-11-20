package com.yarnshipment.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.web.WebTestApp.TestController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Spring'in kendi istek hataları da ERR-07 biçiminde, kodlu ve Türkçe döner; durum kodu değişmez. */
class MvcProblemAdviceTest {

    private final MockMvcTester mvc =
            MockMvcTester.create(WebTestApp.mockMvc(new FakeIdempotencyStore(), new TestController()));

    @Test
    @DisplayName("Yoldaki bozuk kimlik 400 VALIDATION_FAILED döner; tip adı ve İngilizce metin sızmaz")
    void shouldReportMalformedPathId() {
        MvcTestResult result = mvc.get()
                .uri("/things/by-id/not-a-uuid")
                .header("X-Request-Id", "req-7")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("VALIDATION_FAILED");
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("id");
        assertThat(result).bodyJson().extractingPath("$.requestId").isEqualTo("req-7");
        assertThat(result).bodyText().doesNotContain("UUID", "Failed to convert");
    }

    @Test
    @DisplayName("Eksik sorgu parametresi 400 VALIDATION_FAILED ve parametre adıyla döner")
    void shouldReportMissingParameter() {
        MvcTestResult result = mvc.get().uri("/things/search").exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("VALIDATION_FAILED");
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("term");
        assertThat(result).bodyJson().extractingPath("$.errors[0].message").isEqualTo("required");
    }

    @Test
    @DisplayName("Gövdedeki geçersiz alan 400 VALIDATION_FAILED ve alan adıyla döner")
    void shouldReportInvalidBodyField() {
        MvcTestResult result = mvc.post()
                .uri("/things/valid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\" \"}")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("VALIDATION_FAILED");
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("name");
    }

    @Test
    @DisplayName("Desteklenmeyen yöntem 405 METHOD_NOT_ALLOWED döner ve Allow başlığını korur")
    void shouldReportUnsupportedMethod() {
        MvcTestResult result = mvc.delete().uri("/things/search").exchange();

        assertThat(result).hasStatus(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(result).hasHeader("Allow", "GET");
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("METHOD_NOT_ALLOWED");
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("İşlem desteklenmiyor");
    }

    @Test
    @DisplayName("Desteklenmeyen içerik türü 415 UNSUPPORTED_MEDIA_TYPE döner")
    void shouldReportUnsupportedMediaType() {
        MvcTestResult result = mvc.post()
                .uri("/things/json")
                .contentType(MediaType.APPLICATION_XML)
                .content("<a/>")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("UNSUPPORTED_MEDIA_TYPE");
    }
}
