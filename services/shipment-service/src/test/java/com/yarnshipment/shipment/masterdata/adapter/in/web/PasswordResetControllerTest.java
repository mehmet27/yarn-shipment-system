package com.yarnshipment.shipment.masterdata.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yarnshipment.shipment.masterdata.application.port.in.account.ConfirmPasswordResetUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.RequestPasswordResetUseCase;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidResetCodeException;
import com.yarnshipment.shipment.masterdata.domain.model.SmsUnavailableException;
import com.yarnshipment.shipment.masterdata.domain.model.WeakPasswordException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** "Şifremi unuttum" uçlarının sözleşmesi: girişsiz çağrılır, yanıt hesabı sızdırmaz, hatalar kodla eşlenir. */
@WebMvcTest(controllers = PasswordResetController.class)
class PasswordResetControllerTest {

    private static final String REQUEST_URI = "/api/v1/password-reset/request";
    private static final String CONFIRM_URI = "/api/v1/password-reset/confirm";
    private static final String CONFIRM_BODY =
            "{\"username\":\"ali.yilmaz\",\"code\":\"123456\",\"newPassword\":\"Yeni-Sifre-2026\"}";

    private final MockMvcTester mvc;

    @MockitoBean
    private RequestPasswordResetUseCase requestReset;

    @MockitoBean
    private ConfirmPasswordResetUseCase confirmReset;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    PasswordResetControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Kod isteği token olmadan 202 döner, gövdesizdir ve önbelleğe alınmaz")
    void shouldAcceptRequestWithoutLogin() {
        MvcTestResult result = post(REQUEST_URI, "{\"username\":\"ali.yilmaz\"}");

        assertThat(result).hasStatus(HttpStatus.ACCEPTED);
        assertThat(result).bodyText().isEmpty();
        assertThat(result).headers().hasValue("Cache-Control", "no-store");
        verify(requestReset).requestPasswordReset("ali.yilmaz");
    }

    @Test
    @DisplayName("SMS gönderilemezse kod isteği 502 SMS_UNAVAILABLE olur")
    void shouldMapSmsFailure() {
        doThrow(new SmsUnavailableException()).when(requestReset).requestPasswordReset(any());

        MvcTestResult result = post(REQUEST_URI, "{\"username\":\"ali.yilmaz\"}");

        assertThat(result).hasStatus(HttpStatus.BAD_GATEWAY);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("SMS_UNAVAILABLE");
    }

    @Test
    @DisplayName("Doğru kodla yeni şifre belirlenir: 204, token gerekmez, şifre yanıtta ya da loglarda dönmez")
    void shouldConfirmWithoutLogin() {
        MvcTestResult result = post(CONFIRM_URI, CONFIRM_BODY);

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        verify(confirmReset).confirmPasswordReset("ali.yilmaz", "123456", "Yeni-Sifre-2026");
    }

    @Test
    @DisplayName("Yanlış kod ve zayıf şifre 400 koduyla eşlenir")
    void shouldMapRejections() {
        doThrow(new InvalidResetCodeException())
                .doThrow(new WeakPasswordException())
                .when(confirmReset)
                .confirmPasswordReset(any(), any(), any());

        MvcTestResult wrongCode = post(CONFIRM_URI, CONFIRM_BODY);
        MvcTestResult weak = post(CONFIRM_URI, CONFIRM_BODY);

        assertThat(wrongCode).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(wrongCode).bodyJson().extractingPath("$.code").isEqualTo("INVALID_RESET_CODE");
        assertThat(weak).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(weak).bodyJson().extractingPath("$.code").isEqualTo("WEAK_PASSWORD");
    }

    @Test
    @DisplayName("Altı hane olmayan kod ve kısa şifre şemadan 400 alır; use case çağrılmaz")
    void shouldRejectMalformedConfirmation() {
        MvcTestResult shortCode =
                post(CONFIRM_URI, "{\"username\":\"a\",\"code\":\"12\",\"newPassword\":\"Yeni-Sifre-2026\"}");
        MvcTestResult shortPassword =
                post(CONFIRM_URI, "{\"username\":\"a\",\"code\":\"123456\",\"newPassword\":\"x\"}");

        assertThat(shortCode).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(shortPassword).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(confirmReset);
    }

    private MvcTestResult post(String uri, String body) {
        return mvc.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }
}
