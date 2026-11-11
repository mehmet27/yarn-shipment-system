package com.yarnshipment.bag.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yarnshipment.bag.TestTokens;
import com.yarnshipment.bag.application.port.in.BagNotFoundException;
import com.yarnshipment.bag.application.port.in.GetBagUseCase;
import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.bag.domain.model.BagStatus;
import com.yarnshipment.bag.domain.model.PackageType;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Çuval uçlarının sözleşmesi, hata kodları ve izin denetimi. */
@WebMvcTest(controllers = BagController.class)
class BagControllerTest {

    private final MockMvcTester mvc;

    @MockitoBean
    private GetBagUseCase getBagUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    BagControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Çuval alanları döner")
    void shouldReturnBag() {
        // given
        when(getBagUseCase.getBag(any())).thenReturn(bag(BagStatus.CREATED));

        // when
        MvcTestResult result = get("/api/v1/bags/BAG-0001", warehouse());

        // then
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.uniqueKey").isEqualTo("BAG-0001");
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("CREATED");
    }

    @Test
    @DisplayName("Olmayan çuval BAG_NOT_FOUND koduyla 404 döner")
    void shouldReturnProblemForMissingBag() {
        when(getBagUseCase.getBag(any())).thenThrow(new BagNotFoundException("YOK"));

        MvcTestResult result = get("/api/v1/bags/YOK", warehouse());

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("BAG_NOT_FOUND");
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Çuval bulunamadı");
    }

    @Test
    @DisplayName("Şoför rolü çuval okuyamaz")
    void shouldForbidDriver() {
        assertThat(get("/api/v1/bags/BAG-0001", TestTokens.driverToken())).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Token'sız istek 401 alır")
    void shouldRequireAuthentication() {
        assertThat(mvc.get().uri("/api/v1/bags/BAG-0001").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private static Jwt warehouse() {
        return TestTokens.token("warehouse").build();
    }

    private static Bag bag(BagStatus status) {
        return new Bag(UUID.randomUUID(), TestTokens.SITE_ID, "BAG-0001", PackageType.BAG, "P-2026-0001", "A", status);
    }

    private MvcTestResult get(String uri, Jwt jwt) {
        return mvc.get()
                .uri(uri)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .exchange();
    }
}
