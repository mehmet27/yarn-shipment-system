package com.yarnshipment.shipment.masterdata.adapter.out.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.WeakPasswordException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Şifre/durum işlemleri Yönetim API'sini taklit eden gerçek bir HTTP sunucusuna karşı. */
class KeycloakAccountCredentialAdapterTest {

    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-000000000101");
    private static final String USERS = "/admin/realms/yarnshipment/users";
    private static final String USER_PATH = USERS + "/" + USER;
    private static final int OK = 200;
    private static final int NO_CONTENT = 204;
    private static final int BAD_REQUEST = 400;
    private static final int UPDATE_CALL = 3;
    private static final int NOT_FOUND = 404;
    private static final String USER_JSON = "{\"id\":\"" + USER + "\",\"username\":\"ali.yilmaz\",\"enabled\":true,"
            + "\"requiredActions\":[\"VERIFY_EMAIL\"]}";

    private final List<String> calls = new ArrayList<>();
    private final List<String> bodies = new ArrayList<>();
    private @Nullable HttpServer server;
    private String searchResult = "[" + USER_JSON + "]";
    private int userStatus = OK;
    private int resetStatus = NO_CONTENT;

    @AfterEach
    void stopServer() {
        HttpServer running = server;
        if (running != null) {
            running.stop(0);
        }
    }

    @Test
    @DisplayName("Geçici şifre belirlenir, UPDATE_PASSWORD zorunlu eylemi eklenir (diğerleri kalır), oturumlar kapanır")
    void shouldSetTemporaryPassword() throws Exception {
        KeycloakAccountCredentialAdapter adapter = adapter();

        adapter.setTemporaryPassword(USER, "Yeni-Sifre-2026");

        assertThat(calls)
                .containsExactly(
                        "POST /realms/yarnshipment/protocol/openid-connect/token",
                        "PUT " + USER_PATH + "/reset-password",
                        "GET " + USER_PATH,
                        "PUT " + USER_PATH,
                        "POST " + USER_PATH + "/logout");
        assertThat(bodies.get(1)).contains("\"temporary\":true").contains("Yeni-Sifre-2026");
        assertThat(bodies.get(UPDATE_CALL)).contains("VERIFY_EMAIL").contains("UPDATE_PASSWORD");
    }

    @Test
    @DisplayName("Keycloak şifreyi kurala uymadığı için reddederse WeakPasswordException olur")
    void shouldReportWeakPassword() throws Exception {
        resetStatus = BAD_REQUEST;
        KeycloakAccountCredentialAdapter adapter = adapter();

        assertThatThrownBy(() -> adapter.setTemporaryPassword(USER, "kisa")).isInstanceOf(WeakPasswordException.class);

        assertThat(calls).noneMatch(call -> call.endsWith("/logout"));
    }

    @Test
    @DisplayName("Hesap kapatılınca durum yazılır ve oturumlar kapanır; açılınca oturum işlemi yapılmaz")
    void shouldSwitchEnabled() throws Exception {
        KeycloakAccountCredentialAdapter adapter = adapter();

        adapter.disable(USER);
        adapter.enable(USER);

        assertThat(calls).filteredOn(call -> call.endsWith("/logout")).hasSize(1);
        assertThat(bodies).anyMatch(body -> body.equals("{\"enabled\":false}"));
        assertThat(bodies).anyMatch(body -> body.equals("{\"enabled\":true}"));
    }

    @Test
    @DisplayName("Olmayan kullanıcıyı kapatmak LoginAccountNotFoundException verir")
    void shouldReportMissingUserOnDisable() throws Exception {
        userStatus = NOT_FOUND;
        KeycloakAccountCredentialAdapter adapter = adapter();

        assertThatThrownBy(() -> adapter.disable(USER)).isInstanceOf(LoginAccountNotFoundException.class);
    }

    private KeycloakAccountCredentialAdapter adapter() throws IOException {
        HttpServer created = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server = created;
        created.createContext("/", exchange -> {
            calls.add(
                    exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            answer(exchange);
        });
        created.start();
        return new KeycloakAccountCredentialAdapter(new KeycloakAdminClient(
                "http://localhost:" + created.getAddress().getPort(), "yarnshipment", "yarnshipment-admin", "secret"));
    }

    private void answer(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        if (path.endsWith("/token")) {
            reply(exchange, OK, "{\"access_token\":\"t\"}");
        } else if (method.equals("GET") && path.equals(USERS)) {
            reply(exchange, OK, searchResult);
        } else if (method.equals("GET")) {
            reply(exchange, userStatus, userStatus == OK ? USER_JSON : "");
        } else if (method.equals("PUT") && path.endsWith("/reset-password")) {
            reply(exchange, resetStatus, "");
        } else {
            reply(exchange, NO_CONTENT, "");
        }
    }

    private static void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, status == NO_CONTENT || bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0 && status != NO_CONTENT) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }
}
