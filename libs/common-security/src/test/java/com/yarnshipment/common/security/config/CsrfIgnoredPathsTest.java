package com.yarnshipment.common.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.security.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** {@code csrf-ignored-paths} yalnızca adı geçen yolun CSRF belirtecini kaldırır; diğer uçlar korunur. */
@SpringBootTest(
        classes = SecurityTestApplication.class,
        properties = {
            "yarnshipment.security.public-paths=/test/signed/**",
            "yarnshipment.security.csrf-ignored-paths=/test/signed/**"
        })
@AutoConfigureMockMvc
class CsrfIgnoredPathsTest {

    private final MockMvcTester mvc;

    @Autowired
    CsrfIgnoredPathsTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Açık ve CSRF'si kapalı yola oturumsuz, belirteçsiz PUT geçer")
    void shouldAllowSignedUrlWithoutSessionOrToken() {
        MvcTestResult result = mvc.put().uri("/test/signed/upload").exchange();

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("Listede olmayan uç CSRF belirteci olmadan yine 403 alır")
    void shouldKeepCsrfOnOtherPaths() {
        MvcTestResult result = mvc.post()
                .uri("/test/shipments")
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(
                        TestTokens.withRoles("warehouse").build())))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
    }
}
