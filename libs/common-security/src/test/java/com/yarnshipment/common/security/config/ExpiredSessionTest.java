package com.yarnshipment.common.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.security.ActorAuthenticationToken;
import com.yarnshipment.common.security.TestTokens;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Sonlandırılan (süresi doldurulan) oturumun sahibi 200 ve düz metin değil, 401 ve problem cevabı alır. */
@SpringBootTest(classes = SecurityTestApplication.class)
@AutoConfigureMockMvc
class ExpiredSessionTest {

    private final MockMvcTester mvc;
    private final SessionRegistry registry;

    @Autowired
    ExpiredSessionTest(MockMvcTester mvc, SessionRegistry registry) {
        this.mvc = mvc;
        this.registry = registry;
    }

    @Test
    @DisplayName("Sonlandırılan oturumla gelen istek 401 AUTHENTICATION_REQUIRED alır")
    void shouldReturnUnauthorizedForExpiredSession() {
        Jwt jwt = TestTokens.withRoles("warehouse").build();
        ActorAuthenticationToken token = TestTokens.CONVERTER.convert(jwt);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, new SecurityContextImpl(token));
        registry.registerNewSession(session.getId(), token.getPrincipal());

        MvcTestResult open = mvc.get().uri("/test/shipments").session(session).exchange();
        Objects.requireNonNull(registry.getSessionInformation(session.getId())).expireNow();
        MvcTestResult expired =
                mvc.get().uri("/test/shipments").session(session).exchange();

        assertThat(open.getResponse().getStatus()).isNotEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(expired)
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(expired).bodyJson().extractingPath("$.code").isEqualTo("AUTHENTICATION_REQUIRED");
    }
}
