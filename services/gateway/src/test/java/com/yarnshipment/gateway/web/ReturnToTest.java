package com.yarnshipment.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;

/** Giriş, başlatan sayfaya döner; yalnızca SPA'nın kendi adreslerine (açık yönlendirme yok). */
class ReturnToTest {

    private static final String APP = "http://localhost:5173/";
    private static final String LOGIN = "/oauth2/authorization/keycloak";

    private final ReturnTarget target = new ReturnTarget(APP);

    @Test
    @DisplayName("Köke göreli yol SPA adresine çözülür")
    void shouldResolveRelativePath() {
        assertThat(target.resolve("/shipments/42?tab=seal")).contains("http://localhost:5173/shipments/42?tab=seal");
    }

    @Test
    @DisplayName("Aynı kökenli mutlak adres kabul edilir")
    void shouldAcceptSameOrigin() {
        assertThat(target.resolve("http://localhost:5173/depo")).contains("http://localhost:5173/depo");
    }

    @Test
    @DisplayName("Başka köken, şemasız çift eğik çizgi, ters eğik çizgi ve boş değer reddedilir")
    void shouldRejectForeignOrMalformed() {
        assertThat(target.resolve("https://evil.example/x")).isEmpty();
        assertThat(target.resolve("http://localhost:5173.evil.example/")).isEmpty();
        assertThat(target.resolve("//evil.example/x")).isEmpty();
        assertThat(target.resolve("/\\evil.example")).isEmpty();
        assertThat(target.resolve("http://[bad")).isEmpty();
        assertThat(target.resolve(" ")).isEmpty();
        assertThat(target.resolve(null)).isEmpty();
    }

    @Test
    @DisplayName("Girişi başlatan sayfa girişten sonra yeniden açılır ve bir kez kullanılır")
    void shouldRedirectBackOnce() throws Exception {
        MockHttpServletRequest start = new MockHttpServletRequest("GET", LOGIN);
        start.setRequestURI(LOGIN);
        start.setParameter("returnTo", "/shipments/42");
        new ReturnToFilter(APP).doFilter(start, new MockHttpServletResponse(), new MockFilterChain());

        MockHttpServletRequest callback = new MockHttpServletRequest();
        callback.setSession(start.getSession());
        MockHttpServletResponse first = new MockHttpServletResponse();
        ReturnToSuccessHandler handler = new ReturnToSuccessHandler(APP);
        handler.onAuthenticationSuccess(callback, first, new TestingAuthenticationToken("u", "p"));
        MockHttpServletResponse second = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(callback, second, new TestingAuthenticationToken("u", "p"));

        assertThat(first.getRedirectedUrl()).isEqualTo("http://localhost:5173/shipments/42");
        assertThat(second.getRedirectedUrl()).isEqualTo(APP);
    }

    @Test
    @DisplayName("Giriş dışındaki isteklerde ve güvensiz adreste oturum açılmaz")
    void shouldNotTouchSessionOtherwise() throws Exception {
        MockHttpServletRequest other = new MockHttpServletRequest("GET", "/api/v1/me");
        other.setParameter("returnTo", "/x");
        MockHttpServletRequest foreign = new MockHttpServletRequest("GET", LOGIN);
        foreign.setRequestURI(LOGIN);
        foreign.setParameter("returnTo", "https://evil.example/");
        ReturnToFilter filter = new ReturnToFilter(APP);

        filter.doFilter(other, new MockHttpServletResponse(), new MockFilterChain());
        filter.doFilter(foreign, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(other.getSession(false)).isNull();
        assertThat(foreign.getSession(false)).isNull();
    }
}
