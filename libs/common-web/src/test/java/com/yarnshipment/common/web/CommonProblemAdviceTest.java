package com.yarnshipment.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.web.WebTestApp.TestController;
import com.yarnshipment.common.web.problem.CommonProblemAdvice;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.constraints.Max;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class CommonProblemAdviceTest {

    private static final int TOO_LARGE = 101;
    private static final int LIMIT = 100;

    private final MockMvcTester mvc =
            MockMvcTester.create(WebTestApp.mockMvc(new FakeIdempotencyStore(), new TestController()));

    @Test
    @DisplayName("Eksik başlık ERR-07 biçiminde 400 ve MISSING_HEADER döner")
    void shouldReportMissingHeader() {
        MvcTestResult result = mvc.post()
                .uri("/things/needs-header")
                .header("X-Request-Id", "req-1")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("MISSING_HEADER");
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("X-Needed");
        assertThat(result).bodyJson().extractingPath("$.requestId").isEqualTo("req-1");
        assertThat(result)
                .bodyJson()
                .extractingPath("$.type")
                .isEqualTo("https://errors.yarnshipment.com/missing-header");
    }

    @Test
    @DisplayName("Okunamayan gövde 400 ve MALFORMED_REQUEST döner")
    void shouldReportUnreadableBody() {
        MvcTestResult result = mvc.post()
                .uri("/things/json")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("MALFORMED_REQUEST");
    }

    @Test
    @DisplayName("Sınır ihlali VALIDATION_FAILED ve alan listesi ile döner")
    void shouldReportConstraintViolation() {
        ConstraintViolationException exception;
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            exception = new ConstraintViolationException(factory.getValidator().validate(new Paged(TOO_LARGE)));
        }
        CommonProblemAdvice advice = new CommonProblemAdvice(WebTestApp.PROBLEMS);

        ProblemDetail problem = advice.handleConstraintViolation(exception, new MockHttpServletRequest("GET", "/x"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getProperties()).containsEntry("code", "VALIDATION_FAILED");
        assertThat(problem.getProperties()).containsKey("errors");
    }

    private record Paged(@Max(LIMIT) int size) {}
}
