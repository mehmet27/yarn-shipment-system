package com.yarnshipment.app;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Uygulamaya gerçek kullanıcı gibi bağlanır: yönetici ilk kurulum şifresiyle girer, personel hesaplarını açar; herkes
 * giriş ucundan oturum alır, geçici şifresini değiştirir ve yazma isteklerinde CSRF belirteci gönderir.
 */
final class FactoryHttp {

    static final String BOOTSTRAP_PASSWORD = "Ilk-Kurulum-Sifresi-1";

    private static final String PASSWORD = "Fabrika-Test-Sifresi-2";
    private static final String CSRF = "factory-test-csrf-token";
    private static final int RUN_LENGTH = 8;
    private static final String RUN = UUID.randomUUID().toString().substring(0, RUN_LENGTH);

    private final MockMvcTester mvc;
    private final JsonMapper json;
    private final Map<Staff, MockHttpSession> sessions = new EnumMap<>(Staff.class);

    FactoryHttp(MockMvcTester mvc, JsonMapper json) {
        this.mvc = mvc;
        this.json = json;
    }

    static String unique(String label) {
        return label + " " + RUN + "-" + UUID.randomUUID().toString().substring(0, RUN_LENGTH);
    }

    Reply get(Staff staff, String uri, Object... variables) {
        return reply(mvc.get().uri(uri, variables).session(session(staff)).exchange());
    }

    Reply send(Staff staff, HttpMethod method, String uri, @Nullable Object body) {
        return exchange(session(staff), method, uri, body);
    }

    /** İmzalı yükleme adresine dosyayı koyar; adres oturum ve CSRF istemez. */
    void upload(String url, String mimeType, byte[] content) {
        MockHttpServletResponse response = mvc.put()
                .uri(URI.create(url).getRawPath())
                .contentType(MediaType.parseMediaType(mimeType))
                .content(content)
                .exchange()
                .getResponse();
        assertThat(HttpStatus.valueOf(response.getStatus()).is2xxSuccessful())
                .as("upload succeeded")
                .isTrue();
    }

    private synchronized MockHttpSession session(Staff staff) {
        MockHttpSession existing = sessions.get(staff);
        if (existing != null) {
            return existing;
        }
        String username = "admin";
        String firstPassword = BOOTSTRAP_PASSWORD;
        if (staff != Staff.ADMIN) {
            username = staff.name().toLowerCase(Locale.ROOT) + "-" + RUN;
            firstPassword = "Gecici-Sifre-" + RUN;
            createAccount(staff, username, firstPassword);
        }
        MockHttpSession session = login(username, firstPassword);
        sessions.put(staff, session);
        return session;
    }

    private void createAccount(Staff staff, String username, String password) {
        Map<String, Object> account = new LinkedHashMap<>();
        account.put("username", username);
        account.put("fullName", "Test " + staff.name());
        account.put("role", staff.role);
        if (staff.workArea != null) {
            account.put("workArea", staff.workArea);
        }
        account.put("password", password);
        send(Staff.ADMIN, HttpMethod.POST, "/api/v1/master-data/login-accounts", account)
                .ok();
    }

    /** Giriş ucundan oturum açar; geçici şifreyle girildiyse şifreyi değiştirip yeni şifreyle yeniden girer. */
    private MockHttpSession login(String username, String password) {
        MockHttpSession session = new MockHttpSession();
        Reply first = exchange(session, HttpMethod.POST, "/api/v1/auth/login", credentials(username, password));
        if (first.status() != HttpStatus.OK.value() && !password.equals(PASSWORD)) {
            return login(username, PASSWORD);
        }
        first.ok();
        if (!first.body().required("passwordChangeRequired").asBoolean()) {
            return session;
        }
        Map<String, String> change = Map.of("currentPassword", password, "newPassword", PASSWORD);
        exchange(session, HttpMethod.PUT, "/api/v1/auth/password", change).ok();
        return login(username, PASSWORD);
    }

    private static Map<String, String> credentials(String username, String password) {
        return Map.of("username", username, "password", password);
    }

    private Reply exchange(MockHttpSession session, HttpMethod method, String uri, @Nullable Object body) {
        MockMvcTester.MockMvcRequestBuilder request = mvc.method(method)
                .uri(uri)
                .session(session)
                .cookie(new Cookie("XSRF-TOKEN", CSRF))
                .header("X-XSRF-TOKEN", CSRF)
                .header("Idempotency-Key", UUID.randomUUID().toString());
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        return reply(request.exchange());
    }

    private Reply reply(MvcTestResult result) {
        MockHttpServletResponse response = result.getResponse();
        String content = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
        JsonNode body = content.isBlank() ? json.createObjectNode() : json.readTree(content);
        return new Reply(response.getStatus(), body);
    }

    /** Fabrikadaki roller; kalite, fasoncu kumaşına bakan kalite kullanıcısıdır. */
    enum Staff {
        ADMIN("ADMIN", null),
        MANAGER("MANAGER", null),
        WAREHOUSE("WAREHOUSE", null),
        QUALITY("QUALITY", "SUBCONTRACTOR");

        private final String role;
        private final @Nullable String workArea;

        Staff(String role, @Nullable String workArea) {
            this.role = role;
            this.workArea = workArea;
        }
    }

    /** HTTP yanıtı: durum kodu ve JSON gövdesi (gövde yoksa boş nesne). */
    record Reply(int status, JsonNode body) {

        Reply ok() {
            assertThat(HttpStatus.valueOf(status).is2xxSuccessful())
                    .as("HTTP status=%s body=%s", status, body)
                    .isTrue();
            return this;
        }

        String text(String field) {
            return body.required(field).asString();
        }

        UUID id(String field) {
            return UUID.fromString(text(field));
        }
    }
}
