package com.yarnshipment.shipment.masterdata.adapter.out.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountExistsException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountUnavailableException;
import com.yarnshipment.shipment.masterdata.domain.model.StaffAccountSpec;
import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
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

/** Fabrika içi hesap istemcisi, Yönetim API'sini taklit eden gerçek bir HTTP sunucusuna karşı. */
class KeycloakStaffAccountAdapterTest {

    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");
    private static final int OK = 200;
    private static final int DELETE_BODY_CALL = 4;
    private static final int CREATED = 201;
    private static final int NO_CONTENT = 204;
    private static final int NOT_FOUND = 404;
    private static final int BAD_REQUEST = 400;
    private static final int CONFLICT = 409;
    private static final int SERVER_ERROR = 500;
    private static final StaffAccountSpec STAFF =
            new StaffAccountSpec("depo2", "Ayşe Depo", StaffRole.WAREHOUSE, "Gecici-Sifre-1", List.of(SITE));
    private static final String NEW_ID = "0192f0a0-0000-7000-8000-000000000201";
    private static final String USER_ID = "0192f0a0-0000-7000-8000-000000000101";
    private static final String USERS = "/admin/realms/yarnshipment/users";

    private final List<String> calls = new ArrayList<>();
    private final List<String> bodies = new ArrayList<>();
    private @Nullable HttpServer server;
    private int createStatus = CREATED;
    private int roleStatus = NO_CONTENT;
    private int userRolesStatus = OK;
    private int nameStatus = NO_CONTENT;
    private String userRoles = "[]";

    @AfterEach
    void stopServer() {
        HttpServer running = server;
        if (running != null) {
            running.stop(0);
        }
    }

    @Test
    @DisplayName("Fabrika içi kullanıcı yöneticinin geçici şifresiyle açılır, rol verilir; SMS ya da e-posta istenmez")
    void shouldCreateStaffWithTemporaryPassword() throws Exception {
        KeycloakStaffAccountAdapter adapter = adapter("secret");

        UUID created = adapter.createStaff(STAFF);

        assertThat(created).isEqualTo(UUID.fromString(NEW_ID));
        assertThat(calls)
                .containsExactly(
                        "POST /realms/yarnshipment/protocol/openid-connect/token",
                        "GET /admin/realms/yarnshipment/roles/warehouse",
                        "POST " + USERS,
                        "POST " + USERS + "/" + NEW_ID + "/role-mappings/realm");
        assertThat(bodies.get(2))
                .contains("\"username\":\"depo2\"")
                .contains("\"site_ids\":[\"" + SITE + "\"]")
                .contains("\"value\":\"Gecici-Sifre-1\"")
                .contains("\"temporary\":true")
                .contains("UPDATE_PASSWORD")
                .doesNotContain("phone")
                .doesNotContain("email");
    }

    @Test
    @DisplayName("Rol verilemezse açılan kullanıcı silinir ve LoginAccountUnavailableException fırlar")
    void shouldRemoveStaffWhenRoleAssignmentFails() throws Exception {
        roleStatus = SERVER_ERROR;
        KeycloakStaffAccountAdapter adapter = adapter("secret");

        assertThatThrownBy(() -> adapter.createStaff(STAFF)).isInstanceOf(LoginAccountUnavailableException.class);

        assertThat(calls).contains("DELETE " + USERS + "/" + NEW_ID);
    }

    @Test
    @DisplayName("Giriş adı alınmışsa fabrika içi hesap LoginAccountExistsException verir")
    void shouldReportExistingStaffAccount() throws Exception {
        createStatus = CONFLICT;
        KeycloakStaffAccountAdapter adapter = adapter("secret");

        assertThatThrownBy(() -> adapter.createStaff(STAFF)).isInstanceOf(LoginAccountExistsException.class);
    }

    @Test
    @DisplayName("Şifre kurala uymuyorsa Keycloak 400 verir ve WeakPasswordException fırlar")
    void shouldReportWeakPassword() throws Exception {
        createStatus = BAD_REQUEST;
        KeycloakStaffAccountAdapter adapter = adapter("secret");

        assertThatThrownBy(() -> adapter.createStaff(STAFF)).isInstanceOf(WeakPasswordException.class);
    }

    @Test
    @DisplayName("Rol değişince yeni rol eklenir, eski fabrika içi rol kaldırılır, oturumlar kapatılır")
    void shouldReplaceStaffRole() throws Exception {
        userRoles = "[{\"id\":\"r-9\",\"name\":\"warehouse\"},{\"id\":\"r-0\",\"name\":\"offline_access\"}]";
        KeycloakStaffAccountAdapter adapter = adapter("secret");

        adapter.changeStaffRole(UUID.fromString(USER_ID), StaffRole.QUALITY);

        String roles = USERS + "/" + USER_ID + "/role-mappings/realm";
        assertThat(calls)
                .containsExactly(
                        "POST /realms/yarnshipment/protocol/openid-connect/token",
                        "GET " + roles,
                        "GET /admin/realms/yarnshipment/roles/quality",
                        "POST " + roles,
                        "DELETE " + roles,
                        "POST " + USERS + "/" + USER_ID + "/logout");
        assertThat(bodies.get(DELETE_BODY_CALL)).contains("warehouse").doesNotContain("offline_access");
    }

    @Test
    @DisplayName("Ad soyad değişince yalnızca ad ve soyad profile yazılır; rol ve oturumlara dokunulmaz")
    void shouldChangeStaffName() throws Exception {
        KeycloakStaffAccountAdapter adapter = adapter("secret");

        adapter.changeStaffName(UUID.fromString(USER_ID), "Ayşe Nur Yılmaz");

        assertThat(calls)
                .containsExactly(
                        "POST /realms/yarnshipment/protocol/openid-connect/token", "PUT " + USERS + "/" + USER_ID);
        assertThat(bodies.get(1)).contains("\"firstName\":\"Ayşe Nur\"").contains("\"lastName\":\"Yılmaz\"");
    }

    @Test
    @DisplayName("Ad değişirken kullanıcı Keycloak'ta yoksa LoginAccountNotFoundException")
    void shouldReportMissingUserOnNameChange() throws Exception {
        nameStatus = NOT_FOUND;
        KeycloakStaffAccountAdapter adapter = adapter("secret");

        assertThatThrownBy(() -> adapter.changeStaffName(UUID.fromString(USER_ID), "Ayşe Yılmaz"))
                .isInstanceOf(LoginAccountNotFoundException.class);
    }

    @Test
    @DisplayName("Kullanıcı yoksa LoginAccountNotFoundException")
    void shouldRefuseRoleChangeForBoundOrMissingAccounts() throws Exception {
        userRolesStatus = NOT_FOUND;
        KeycloakStaffAccountAdapter missing = adapter("secret");
        assertThatThrownBy(() -> missing.changeStaffRole(UUID.fromString(USER_ID), StaffRole.ADMIN))
                .isInstanceOf(LoginAccountNotFoundException.class);
    }

    @Test
    @DisplayName("Yönetim istemcisi yapılandırılmamışsa hiçbir çağrı yapılmadan LoginAccountUnavailableException olur")
    void shouldFailWhenNotConfigured() throws Exception {
        KeycloakStaffAccountAdapter adapter = adapter("");

        assertThatThrownBy(() -> adapter.createStaff(STAFF)).isInstanceOf(LoginAccountUnavailableException.class);

        assertThat(calls).isEmpty();
    }

    private KeycloakStaffAccountAdapter adapter(String secret) throws IOException {
        HttpServer created = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server = created;
        created.createContext("/", exchange -> {
            calls.add(
                    exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            answer(exchange);
        });
        created.start();
        return new KeycloakStaffAccountAdapter(new KeycloakAdminClient(
                "http://localhost:" + created.getAddress().getPort(), "yarnshipment", "yarnshipment-admin", secret));
    }

    private void answer(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        if (path.endsWith("/token")) {
            reply(exchange, OK, "{\"access_token\":\"t\"}");
        } else if (method.equals("GET") && path.endsWith("/role-mappings/realm")) {
            reply(exchange, userRolesStatus, userRoles);
        } else if (method.equals("POST") && path.endsWith("/role-mappings/realm")) {
            reply(exchange, roleStatus, "");
        } else {
            answerUser(exchange, method, path);
        }
    }

    private void answerUser(HttpExchange exchange, String method, String path) throws IOException {
        if (method.equals("PUT") && path.equals(USERS + "/" + USER_ID)) {
            reply(exchange, nameStatus, "");
        } else if (method.equals("GET")) {
            reply(exchange, OK, "{\"id\":\"r-1\",\"name\":\"" + path.substring(path.lastIndexOf('/') + 1) + "\"}");
        } else if (method.equals("POST") && path.endsWith("/users")) {
            exchange.getResponseHeaders().add("Location", "http://kc" + USERS + "/" + NEW_ID);
            reply(exchange, createStatus, "");
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
