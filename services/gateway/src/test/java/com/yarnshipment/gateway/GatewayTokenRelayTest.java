package com.yarnshipment.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest(properties = "GATEWAY_CLIENT_SECRET=test-secret")
@AutoConfigureMockMvc
@Import(TestClientRegistrations.class)
class GatewayTokenRelayTest {

    private static final String BODY = "{\"ok\":true}";
    private static final AtomicReference<String> AUTHORIZATION = new AtomicReference<>();
    private static final AtomicReference<String> COOKIE = new AtomicReference<>();
    private static final HttpServer DOWNSTREAM = startDownstream();

    private final MockMvcTester mvc;

    @Autowired
    GatewayTokenRelayTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @DynamicPropertySource
    static void downstreamUrl(DynamicPropertyRegistry registry) {
        registry.add(
                "SHIPMENT_SERVICE_URL",
                () -> "http://localhost:" + DOWNSTREAM.getAddress().getPort());
    }

    @AfterAll
    static void stopDownstream() {
        DOWNSTREAM.stop(0);
    }

    @Test
    @DisplayName("Oturumdaki kullanıcının access token'ı servise Bearer olarak gider, çerez gitmez")
    void shouldRelayBearerTokenAndDropCookie() {
        MvcTestResult result = mvc.get()
                .uri("/api/v1/me")
                .with(SecurityMockMvcRequestPostProcessors.oidcLogin())
                .with(SecurityMockMvcRequestPostProcessors.oauth2Client("keycloak"))
                .cookie(new Cookie("JSESSIONID", "abc"))
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyText().isEqualTo(BODY);
        assertThat(AUTHORIZATION.get()).startsWith("Bearer ");
        assertThat(COOKIE.get()).isNull();
    }

    private static HttpServer startDownstream() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/api/v1/me", GatewayTokenRelayTest::answer);
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void answer(HttpExchange exchange) throws IOException {
        AUTHORIZATION.set(exchange.getRequestHeaders().getFirst("Authorization"));
        COOKIE.set(exchange.getRequestHeaders().getFirst("Cookie"));
        byte[] body = BODY.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(HttpStatus.OK.value(), body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
