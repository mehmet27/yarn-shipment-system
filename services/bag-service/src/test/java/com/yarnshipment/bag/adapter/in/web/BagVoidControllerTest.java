package com.yarnshipment.bag.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yarnshipment.bag.TestTokens;
import com.yarnshipment.bag.application.port.in.VoidBagsUseCase;
import com.yarnshipment.bag.domain.model.BagNotVoidableException;
import com.yarnshipment.bag.domain.model.BagStatus;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Çuval iptal ucunun sözleşmesi, hata kodu ve izin denetimi. */
@WebMvcTest(controllers = BagVoidController.class)
class BagVoidControllerTest {

    private static final String VOID_BODY =
            "{\"uniqueKeys\":[\"BAG-7K3M-Q9XD\"],\"reason\":\"Yanlış sayıda üretildi\"}";

    private final MockMvcTester mvc;

    @MockitoBean
    private VoidBagsUseCase voidBagsUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    BagVoidControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Depo personeli yanlış üretilen çuvalı gerekçeyle iptal eder; iptal edilen sayı döner")
    void shouldVoidBags() {
        when(voidBagsUseCase.voidBags(any())).thenReturn(1);

        MvcTestResult result = post(
                "/api/v1/bags/void", VOID_BODY, TestTokens.token("warehouse").build());

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.voidedCount").isEqualTo(1);
    }

    @Test
    @DisplayName("İptal gerekçesi boşluktan oluşamaz")
    void shouldRejectBlankVoidReason() {
        String body = "{\"uniqueKeys\":[\"BAG-7K3M-Q9XD\"],\"reason\":\"   \"}";

        assertThat(post("/api/v1/bags/void", body, TestTokens.token("warehouse").build()))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Depoda olmayan çuvalın iptali BAG_NOT_VOIDABLE koduyla 409 döner")
    void shouldReturnConflictForShippedBag() {
        when(voidBagsUseCase.voidBags(any()))
                .thenThrow(new BagNotVoidableException("BAG-7K3M-Q9XD", BagStatus.SHIPPED));

        MvcTestResult result = post(
                "/api/v1/bags/void", VOID_BODY, TestTokens.token("warehouse").build());

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("BAG_NOT_VOIDABLE");
    }

    @Test
    @DisplayName("Kalite, yönetici, ve şoför çuval iptal edemez")
    void shouldForbidRolesWithoutBagCreate() {
        for (Jwt jwt : List.of(
                TestTokens.token("quality").claim("amr", List.of("otp")).build(),
                TestTokens.token("manager").build(),
                TestTokens.driverToken())) {
            assertThat(post("/api/v1/bags/void", VOID_BODY, jwt)).hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    private MvcTestResult post(String uri, String body, Jwt jwt) {
        return mvc.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .exchange();
    }
}
