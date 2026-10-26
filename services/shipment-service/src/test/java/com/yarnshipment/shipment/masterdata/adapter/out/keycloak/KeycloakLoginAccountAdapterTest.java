package com.yarnshipment.shipment.masterdata.adapter.out.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountExistsException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSpec;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountUnavailableException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginRole;
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

/** Keycloak istemcisi, Yönetim API'sini taklit eden gerçek bir HTTP sunucusuna karşı. */
class KeycloakLoginAccountAdapterTest {

    private static final UUID OWNER = UUID.fromString("0192f0a0-0000-7000-8000-000000000004");
    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");
    private static final int OK = 200;
    private static final int CREATED = 201;
    private static final int NO_CONTENT = 204;
    private static final int CONFLICT = 409;
    private static final int SERVER_ERROR = 500;
    private static final int USER_CALL = 3;
    private static final String USERS = "/admin/realms/yarnshipment/users";
    private static final LoginAccountSpec SPEC = new LoginAccountSpec(
            "ali.yilmaz", "ali@ornek.com", "Ali Yılmaz", LoginRole.DRIVER, OWNER, List.of(SITE), "Gecici-Sifre-123");

    private final List<String> calls = new ArrayList<>();
    private final List<String> bodies = new ArrayList<>();
    private @Nullable HttpServer server;
    private String existingUsers = "[]";
    private int createStatus = CREATED;
    private int roleStatus = NO_CONTENT;

    @AfterEach
    void stopServer() {
        HttpServer running = server;
        if (running != null) {
            running.stop(0);
        }
    }

    @Test
    @DisplayName("Kullanıcı geçici şifre, öznitelikler ve rolle açılır; sıra: token, arama, rol, kullanıcı, rol atama")
    void shouldCreateUserWithRoleAndAttributes() throws Exception {
        KeycloakLoginAccountAdapter adapter = adapter("secret");

        adapter.create(SPEC);

        assertThat(calls)
                .containsExactly(
                        "POST /realms/yarnshipment/protocol/openid-connect/token",
                        "GET " + USERS,
                        "GET /admin/realms/yarnshipment/roles/driver",
                        "POST " + USERS,
                        "POST " + USERS + "/u-1/role-mappings/realm");
        String user = bodies.get(USER_CALL);
        assertThat(user)
                .contains("\"username\":\"ali.yilmaz\"")
                .contains("\"email\":\"ali@ornek.com\"")
                .contains("\"driver_id\":[\"" + OWNER + "\"]")
                .contains("\"site_ids\":[\"" + SITE + "\"]")
                .contains("\"temporary\":true")
                .contains("UPDATE_PASSWORD");
        assertThat(bodies.get(0)).contains("client_secret=secret");
    }

    @Test
    @DisplayName("Kayıt için hesap varsa ya da giriş adı alınmışsa LoginAccountExistsException; yarım iş kalmaz")
    void shouldReportExistingAccount() throws Exception {
        existingUsers = "[{\"id\":\"u-9\"}]";
        KeycloakLoginAccountAdapter byRecord = adapter("secret");
        assertThatThrownBy(() -> byRecord.create(SPEC)).isInstanceOf(LoginAccountExistsException.class);
        assertThat(calls).doesNotContain("POST " + USERS);
        stopServer();

        existingUsers = "[]";
        createStatus = CONFLICT;
        KeycloakLoginAccountAdapter byName = adapter("secret");
        assertThatThrownBy(() -> byName.create(SPEC)).isInstanceOf(LoginAccountExistsException.class);
    }

    @Test
    @DisplayName("Rol atanamazsa açılan kullanıcı silinir ve LoginAccountUnavailableException fırlar")
    void shouldRemoveUserWhenRoleAssignmentFails() throws Exception {
        roleStatus = SERVER_ERROR;
        KeycloakLoginAccountAdapter adapter = adapter("secret");

        assertThatThrownBy(() -> adapter.create(SPEC)).isInstanceOf(LoginAccountUnavailableException.class);

        assertThat(calls).contains("DELETE " + USERS + "/u-1");
    }

    @Test
    @DisplayName("Yönetim istemcisi yapılandırılmamışsa hiçbir çağrı yapılmadan LoginAccountUnavailableException olur")
    void shouldFailWhenNotConfigured() throws Exception {
        KeycloakLoginAccountAdapter adapter = adapter("");

        assertThatThrownBy(() -> adapter.create(SPEC)).isInstanceOf(LoginAccountUnavailableException.class);

        assertThat(calls).isEmpty();
    }

    private KeycloakLoginAccountAdapter adapter(String secret) throws IOException {
        HttpServer created = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server = created;
        created.createContext("/", exchange -> {
            calls.add(
                    exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            answer(exchange);
        });
        created.start();
        return new KeycloakLoginAccountAdapter(
                "http://localhost:" + created.getAddress().getPort(), "yarnshipment", "yarnshipment-admin", secret);
    }

    private void answer(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        if (path.endsWith("/token")) {
            reply(exchange, OK, "{\"access_token\":\"t\"}");
        } else if (method.equals("GET") && path.endsWith("/users")) {
            reply(exchange, OK, existingUsers);
        } else if (method.equals("GET")) {
            reply(exchange, OK, "{\"id\":\"r-1\",\"name\":\"driver\"}");
        } else if (method.equals("POST") && path.endsWith("/users")) {
            exchange.getResponseHeaders().add("Location", "http://kc" + USERS + "/u-1");
            reply(exchange, createStatus, "");
        } else if (method.equals("POST")) {
            reply(exchange, roleStatus, "");
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
