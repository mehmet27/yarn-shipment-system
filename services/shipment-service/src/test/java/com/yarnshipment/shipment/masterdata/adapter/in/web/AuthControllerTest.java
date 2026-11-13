package com.yarnshipment.shipment.masterdata.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.ActorAuthenticationToken;
import com.yarnshipment.common.security.Role;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.auth.AuthenticateUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.auth.ChangeOwnPasswordUseCase;
import com.yarnshipment.shipment.masterdata.domain.model.AccountLockedException;
import com.yarnshipment.shipment.masterdata.domain.model.AuthenticationFailedException;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidCurrentPasswordException;
import com.yarnshipment.shipment.masterdata.domain.model.SessionUser;
import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Giriş, çıkış ve şifre değiştirme uçlarının sözleşmesi: oturum yazımı, hata kodları ve önbellek başlığı. */
@WebMvcTest(controllers = AuthController.class)
class AuthControllerTest {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String PASSWORD_URI = "/api/v1/auth/password";
    private static final UUID USER_ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000102");
    private static final String LOGIN_BODY = "{\"username\":\"depo\",\"password\":\"Sifre-12345\"}";

    private final MockMvcTester mvc;

    @MockitoBean
    private AuthenticateUseCase authenticate;

    @MockitoBean
    private ChangeOwnPasswordUseCase changePassword;

    @Autowired
    AuthControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Doğru girişte oturum açılır: rol ve izin oturumdaki kimliğe yazılır, yanıt önbelleğe alınmaz")
    void shouldOpenSessionOnLogin() {
        when(authenticate.authenticate("depo", "Sifre-12345")).thenReturn(sessionUser(false));

        MvcTestResult result = login();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.passwordChangeRequired").isEqualTo(false);
        assertThat(result.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).contains("no-store");
        ActorAuthenticationToken token = sessionToken(result);
        assertThat(token.actor().userId()).isEqualTo(USER_ID);
        assertThat(token.actor().roles()).containsExactly(Role.WAREHOUSE);
        assertThat(token.actor().siteIds()).containsExactly(TestTokens.SITE_ID);
        assertThat(token.fullName()).isEqualTo("Davut Depocu");
        assertThat(token.passwordChangeRequired()).isFalse();
    }

    @Test
    @DisplayName("Geçici şifreyle girenin oturumunda rol ve izin yoktur; yalnızca şifresini değiştirebilir")
    void shouldGiveNoPermissionsUntilPasswordChanged() {
        when(authenticate.authenticate("depo", "Sifre-12345")).thenReturn(sessionUser(true));

        MvcTestResult result = login();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.passwordChangeRequired").isEqualTo(true);
        ActorAuthenticationToken token = sessionToken(result);
        assertThat(token.actor().roles()).isEmpty();
        assertThat(token.actor().permissions()).isEmpty();
        assertThat(token.passwordChangeRequired()).isTrue();
    }

    @Test
    @DisplayName("Yanlış girişte 401 AUTHENTICATION_FAILED döner ve oturum açılmaz")
    void shouldRejectWrongCredentials() {
        when(authenticate.authenticate(any(), any())).thenThrow(new AuthenticationFailedException());

        MvcTestResult result = login();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("AUTHENTICATION_FAILED");
        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    @DisplayName("Kilitli hesapta 423 ACCOUNT_LOCKED döner")
    void shouldReportLockedAccount() {
        when(authenticate.authenticate(any(), any())).thenThrow(new AccountLockedException(Instant.now()));

        MvcTestResult result = login();

        assertThat(result).hasStatus(HttpStatus.LOCKED);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("ACCOUNT_LOCKED");
    }

    @Test
    @DisplayName("Eksik gövdeli giriş 400 alır")
    void shouldRejectIncompleteLogin() {
        MvcTestResult result = mvc.post()
                .uri(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"depo\"}")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Çıkış oturum ister; oturumlu istek 204 alır")
    void shouldLogoutOnlyWithSession() {
        MvcTestResult anonymous = mvc.post().uri("/api/v1/auth/logout").exchange();
        MvcTestResult signedIn = mvc.post()
                .uri("/api/v1/auth/logout")
                .with(sessionOf("warehouse"))
                .exchange();

        assertThat(anonymous).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(signedIn).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("Şifre değiştirme oturum ister; oturumlu kullanıcı kendi kimliğiyle değiştirir")
    void shouldChangeOwnPassword() {
        when(changePassword.changeOwnPassword(eq(USER_ID), any(), any())).thenReturn(sessionUser(false));
        String body = "{\"currentPassword\":\"Sifre-12345\",\"newPassword\":\"Yeni-Sifre-678\"}";

        MvcTestResult anonymous = mvc.put()
                .uri(PASSWORD_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
        MvcTestResult signedIn = putPassword(body, sessionOf("warehouse"));

        assertThat(anonymous).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(signedIn).hasStatus(HttpStatus.NO_CONTENT);
        verify(changePassword).changeOwnPassword(USER_ID, "Sifre-12345", "Yeni-Sifre-678");
    }

    @Test
    @DisplayName("Mevcut şifre yanlışsa 400 INVALID_CURRENT_PASSWORD, kısa yeni şifre 400 döner")
    void shouldReportPasswordErrors() {
        when(changePassword.changeOwnPassword(any(), any(), any())).thenThrow(new InvalidCurrentPasswordException());

        MvcTestResult wrong = putPassword(
                "{\"currentPassword\":\"yanlis\",\"newPassword\":\"Yeni-Sifre-678\"}", sessionOf("warehouse"));
        MvcTestResult tooShort =
                putPassword("{\"currentPassword\":\"yanlis\",\"newPassword\":\"kisa\"}", sessionOf("warehouse"));

        assertThat(wrong).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(wrong).bodyJson().extractingPath("$.code").isEqualTo("INVALID_CURRENT_PASSWORD");
        assertThat(tooShort).hasStatus(HttpStatus.BAD_REQUEST);
    }

    private MvcTestResult login() {
        return mvc.post()
                .uri(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(LOGIN_BODY)
                .exchange();
    }

    private MvcTestResult putPassword(String body, RequestPostProcessor session) {
        return mvc.put()
                .uri(PASSWORD_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(session)
                .exchange();
    }

    private static ActorAuthenticationToken sessionToken(MvcTestResult result) {
        SecurityContext context = (SecurityContext) result.getRequest()
                .getSession()
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        return (ActorAuthenticationToken) Objects.requireNonNull(context.getAuthentication());
    }

    private static SessionUser sessionUser(boolean passwordChangeRequired) {
        return new SessionUser(
                USER_ID,
                "depo",
                "Davut Depocu",
                StaffRole.WAREHOUSE,
                Set.of(TestTokens.SITE_ID),
                passwordChangeRequired);
    }

    private static RequestPostProcessor sessionOf(String role) {
        return SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(
                TestTokens.token(role).subject(USER_ID.toString()).build()));
    }
}
