package com.yarnshipment.common.security.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.security.TestTokens;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

class TokenRelayInterceptorTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    @DisplayName("Gelen isteğin token'ı ve request id'si servis çağrısına eklenir")
    void shouldRelayTokenAndRequestId() {
        // given
        Jwt jwt = TestTokens.withRoles("warehouse").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        MDC.put(SecurityProblemResponses.REQUEST_ID_MDC_KEY, "req-42");
        RestClient.Builder builder = RestClient.builder().requestInterceptor(new TokenRelayInterceptor());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue()))
                .andExpect(MockRestRequestMatchers.header(SecurityProblemResponses.REQUEST_ID_HEADER, "req-42"))
                .andRespond(MockRestResponseCreators.withSuccess());

        // when
        builder.build().get().uri("http://bag-service/api/v1/bags").retrieve().toBodilessEntity();

        // then
        server.verify();
    }

    @Test
    @DisplayName("Doğrulanmış istek yoksa Authorization başlığı eklenmez")
    void shouldNotAddAuthorizationWithoutAuthentication() {
        // given
        RestClient.Builder builder = RestClient.builder().requestInterceptor(new TokenRelayInterceptor());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(MockRestRequestMatchers.headerDoesNotExist(HttpHeaders.AUTHORIZATION))
                .andRespond(MockRestResponseCreators.withSuccess());

        // when
        builder.build().get().uri("http://bag-service/api/v1/bags").retrieve().toBodilessEntity();

        // then
        server.verify();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
