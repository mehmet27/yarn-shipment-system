package com.yarnshipment.shipment.masterdata.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.DisableLoginAccountUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.EnableLoginAccountUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.StartPasswordResetUseCase;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.SmsUnavailableException;
import java.util.UUID;
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

/** Hesabı kapatma/açma ve yönetici başlatmalı şifre sıfırlama: yalnızca USER_MANAGE, hatalar kodla eşlenir. */
@WebMvcTest(controllers = LoginAccountAccessController.class)
class LoginAccountAccessControllerTest {

    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-000000000101");
    private static final String ENABLED_URI = "/api/v1/master-data/login-accounts/" + USER + "/enabled";
    private static final String RESET_URI = "/api/v1/master-data/login-accounts/" + USER + "/password-reset";

    private final MockMvcTester mvc;

    @MockitoBean
    private DisableLoginAccountUseCase disableAccount;

    @MockitoBean
    private EnableLoginAccountUseCase enableAccount;

    @MockitoBean
    private StartPasswordResetUseCase startReset;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    LoginAccountAccessControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Yönetici hesabı kapatır: 204 ve use case enabled=false ile çağrılır")
    void shouldDisable() {
        MvcTestResult result = put(
                ENABLED_URI, "{\"enabled\":false}", TestTokens.token("admin").build());

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        verify(disableAccount).disableLoginAccount(any(), eq(USER));
    }

    @Test
    @DisplayName("Yönetici hesabı yeniden açar: 204 ve use case enable ile çağrılır")
    void shouldEnable() {
        MvcTestResult result =
                put(ENABLED_URI, "{\"enabled\":true}", TestTokens.token("admin").build());

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        verify(enableAccount).enableLoginAccount(any(), eq(USER));
    }

    @Test
    @DisplayName("Kendi hesabını kapatma 400, kullanıcı yok 404, SMS yok 502 koduyla eşlenir")
    void shouldMapErrors() {
        Jwt admin = TestTokens.token("admin").build();
        doThrow(new InvalidMasterDataException("userId", "own account"))
                .doThrow(new LoginAccountNotFoundException())
                .when(disableAccount)
                .disableLoginAccount(any(), any());
        doThrow(new SmsUnavailableException()).when(startReset).startPasswordReset(any(), any());

        MvcTestResult own = put(ENABLED_URI, "{\"enabled\":false}", admin);
        MvcTestResult missing = put(ENABLED_URI, "{\"enabled\":false}", admin);
        MvcTestResult smsDown = post(RESET_URI, admin);

        assertThat(own).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(missing).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(smsDown).hasStatus(HttpStatus.BAD_GATEWAY);
        assertThat(smsDown).bodyJson().extractingPath("$.code").isEqualTo("SMS_UNAVAILABLE");
    }

    @Test
    @DisplayName("Yönetici sıfırlamayı başlatır: 202, gövdesiz ve önbelleksiz; şifre dönmez")
    void shouldStartReset() {
        MvcTestResult result = post(RESET_URI, TestTokens.token("admin").build());

        assertThat(result).hasStatus(HttpStatus.ACCEPTED);
        assertThat(result).bodyText().isEmpty();
        assertThat(result).headers().hasValue("Cache-Control", "no-store");
        verify(startReset).startPasswordReset(any(), eq(USER));
    }

    @Test
    @DisplayName("Admin dışı roller 403 alır; use case çağrılmaz")
    void shouldForbidOtherRoles() {
        Jwt warehouse = TestTokens.token("warehouse").build();

        MvcTestResult disable = put(ENABLED_URI, "{\"enabled\":false}", warehouse);
        MvcTestResult reset = post(RESET_URI, warehouse);

        assertThat(disable).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(reset).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(disableAccount, enableAccount, startReset);
    }

    private MvcTestResult put(String uri, String body, Jwt jwt) {
        return mvc.put()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .exchange();
    }

    private MvcTestResult post(String uri, Jwt jwt) {
        return mvc.post()
                .uri(uri)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .exchange();
    }
}
