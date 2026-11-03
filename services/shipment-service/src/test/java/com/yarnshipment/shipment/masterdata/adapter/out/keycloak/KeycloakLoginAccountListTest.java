package com.yarnshipment.shipment.masterdata.adapter.out.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSummary;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountUnavailableException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Giriş hesabı listesi, Keycloak Yönetim API'sini taklit eden gerçek bir HTTP sunucusuna karşı. */
class KeycloakLoginAccountListTest {

    private static final int OK = 200;
    private static final int NOT_FOUND = 404;
    private static final int SERVER_ERROR = 500;
    private static final int PAGE_SIZE = 100;
    private static final long CREATED_MILLIS = 1_767_225_600_000L;
    private static final String ROLES = "/admin/realms/yarnshipment/roles/";
    private static final String ADMIN_USER = "{\"id\":\"u-1\",\"username\":\"admin\",\"firstName\":\"Sistem\","
            + "\"lastName\":\"Yöneticisi\",\"email\":\"admin@fabrika.com\",\"enabled\":true,"
            + "\"createdTimestamp\":" + CREATED_MILLIS + ",\"requiredActions\":[]}";
    private static final String DRIVER_USER = "{\"id\":\"u-2\",\"username\":\"ali.yilmaz\",\"firstName\":\"Ali\","
            + "\"lastName\":\"Ali\",\"enabled\":false,\"requiredActions\":[\"UPDATE_PASSWORD\"]}";

    private final List<String> calls = new ArrayList<>();
    private final Map<String, String> members = new HashMap<>();
    private @Nullable HttpServer server;
    private int roleStatus = OK;

    @AfterEach
    void stopServer() {
        HttpServer running = server;
        if (running != null) {
            running.stop(0);
        }
    }

    @Test
    @DisplayName("Rol üyeleri tek kullanıcıda birleşir, giriş adına göre sıralanır; SYSTEM rolü sorulmaz")
    void shouldMergeRoleMembersIntoAccounts() throws Exception {
        members.put("admin", "[" + ADMIN_USER + "]");
        members.put("quality", "[" + ADMIN_USER + "]");
        members.put("driver", "[" + DRIVER_USER + "]");

        List<LoginAccountSummary> accounts = adapter("secret").findAll();

        assertThat(accounts).extracting(LoginAccountSummary::username).containsExactly("admin", "ali.yilmaz");
        LoginAccountSummary admin = accounts.get(0);
        assertThat(admin.fullName()).isEqualTo("Sistem Yöneticisi");
        assertThat(admin.email()).isEqualTo("admin@fabrika.com");
        assertThat(admin.roles()).containsExactly("ADMIN", "QUALITY");
        assertThat(admin.enabled()).isTrue();
        assertThat(admin.passwordChangeRequired()).isFalse();
        assertThat(admin.createdAt()).isEqualTo(Instant.ofEpochMilli(CREATED_MILLIS));
        LoginAccountSummary driver = accounts.get(1);
        assertThat(driver.fullName()).isEqualTo("Ali");
        assertThat(driver.email()).isNull();
        assertThat(driver.roles()).containsExactly("DRIVER");
        assertThat(driver.enabled()).isFalse();
        assertThat(driver.passwordChangeRequired()).isTrue();
        assertThat(calls).first().isEqualTo("POST /realms/yarnshipment/protocol/openid-connect/token");
        assertThat(calls)
                .contains("GET " + ROLES + "driver/users?first=0&max=100&briefRepresentation=false")
                .noneMatch(call -> call.contains("/roles/system/"));
    }

    @Test
    @DisplayName("Dolu gelen sayfadan sonra sonraki sayfa istenir; realm'de olmayan rol atlanır")
    void shouldPageThroughMembersAndSkipMissingRoles() throws Exception {
        String fullPage = IntStream.range(0, PAGE_SIZE)
                .mapToObj(i -> "{\"id\":\"w-" + i + "\",\"username\":\"depo" + i + "\",\"enabled\":true}")
                .collect(Collectors.joining(",", "[", "]"));
        members.put("warehouse", fullPage);
        members.put("warehouse@100", "[{\"id\":\"w-x\",\"username\":\"depo-son\",\"enabled\":true}]");
        members.put("manager", "missing");

        List<LoginAccountSummary> accounts = adapter("secret").findAll();

        assertThat(accounts).hasSize(PAGE_SIZE + 1);
        assertThat(calls).contains("GET " + ROLES + "warehouse/users?first=100&max=100&briefRepresentation=false");
    }

    @Test
    @DisplayName("Keycloak hata verirse ya da yapılandırılmamışsa LoginAccountUnavailableException olur")
    void shouldReportUnavailableKeycloak() throws Exception {
        assertThatThrownBy(() -> adapter("").findAll()).isInstanceOf(LoginAccountUnavailableException.class);
        assertThat(calls).isEmpty();
        stopServer();

        roleStatus = SERVER_ERROR;
        KeycloakLoginAccountAdapter failing = adapter("secret");
        assertThatThrownBy(failing::findAll).isInstanceOf(LoginAccountUnavailableException.class);
    }

    private KeycloakLoginAccountAdapter adapter(String secret) throws IOException {
        HttpServer created = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server = created;
        created.createContext("/", exchange -> {
            String query = exchange.getRequestURI().getRawQuery();
            calls.add(exchange.getRequestMethod() + " "
                    + exchange.getRequestURI().getPath()
                    + (query == null ? "" : "?" + query));
            answer(exchange);
        });
        created.start();
        return new KeycloakLoginAccountAdapter(
                "http://localhost:" + created.getAddress().getPort(), "yarnshipment", "yarnshipment-admin", secret);
    }

    private void answer(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.endsWith("/token")) {
            reply(exchange, OK, "{\"access_token\":\"t\"}");
        } else if (roleStatus != OK) {
            reply(exchange, roleStatus, "");
        } else {
            String role = path.substring(ROLES.length(), path.lastIndexOf("/users"));
            String query = exchange.getRequestURI().getRawQuery();
            String key = query != null && query.contains("first=" + PAGE_SIZE) ? role + "@" + PAGE_SIZE : role;
            String body = members.getOrDefault(key, "[]");
            boolean missing = body.equals("missing");
            reply(exchange, missing ? NOT_FOUND : OK, missing ? "" : body);
        }
    }

    private static void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }
}
