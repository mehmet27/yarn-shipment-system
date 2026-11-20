package com.yarnshipment.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroups;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Gerçek sunucuya karşı: sağlık ucu veritabanını denetler ama ayrıntı vermez; filtre zincirinden ve Spring MVC'den
 * dönen hatalar da proje biçimindedir (ERR-07), İngilizce varsayılan gövde ya da teknik ayrıntı dönmez.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "yarnshipment.evidence.storage.signing-key=test-signing-key-test-signing-key-0123",
            "yarnshipment.evidence.storage.root=target/evidence-test"
        })
class HealthAndErrorResponseTest {

    private static final int OK = 200;
    private static final int BAD_REQUEST = 400;
    private static final int UNAUTHORIZED = 401;
    private static final int METHOD_NOT_ALLOWED = 405;
    private static final int UNSUPPORTED_MEDIA_TYPE = 415;
    private static final String LOGIN = "/api/v1/auth/login";
    private static final String JSON = "application/json";
    private static final String CONTENT_TYPE = "Content-Type";

    private final HttpClient client = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private HealthEndpoint healthEndpoint;

    @Autowired
    private HealthEndpointGroups healthGroups;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @Test
    @DisplayName("Sağlık ucu oturumsuz çağrıya yalnızca durumu döner; bileşen ve ayrıntı vermez")
    void shouldExposeOnlyStatusToAnonymousCaller() throws Exception {
        HttpResponse<String> response =
                send(HttpRequest.newBuilder(uri("/actuator/health")).GET());

        assertThat(response.statusCode()).isEqualTo(OK);
        JsonNode body = JsonMapper.shared().readTree(response.body());
        assertThat(body.path("status").asString()).isEqualTo("UP");
        assertThat(body.has("components")).isFalse();
        assertThat(body.has("details")).isFalse();
        assertThat(response.body()).doesNotContain("PostgreSQL", "diskSpace", "jdbc");
    }

    @Test
    @DisplayName("Sağlık denetimi veritabanını kapsar: db göstergesi UP ve readiness grubunun üyesidir")
    void shouldCheckDatabaseInHealthAndReadiness() throws Exception {
        HealthDescriptor database = Objects.requireNonNull(healthEndpoint.healthForPath("db"), "db health");

        assertThat(database.getStatus()).isEqualTo(Status.UP);
        assertThat(Objects.requireNonNull(healthGroups.get("readiness")).isMember("db"))
                .isTrue();
        assertThat(Objects.requireNonNull(healthGroups.get("liveness")).isMember("db"))
                .isFalse();
        HttpResponse<String> readiness =
                send(HttpRequest.newBuilder(uri("/actuator/health/readiness")).GET());
        assertThat(readiness.statusCode()).isEqualTo(OK);
        assertThat(readiness.body()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("Sağlık dışındaki yönetim uçları açık değildir")
    void shouldNotExposeOtherManagementEndpoints() throws Exception {
        HttpResponse<String> response =
                send(HttpRequest.newBuilder(uri("/actuator/env")).GET());

        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED);
        assertThat(code(response)).isEqualTo("AUTHENTICATION_REQUIRED");
    }

    @Test
    @DisplayName("Oturumsuz API isteği 401 ve proje biçiminde hata döner")
    void shouldReturnProblemForAnonymousApiCall() throws Exception {
        HttpResponse<String> response =
                send(HttpRequest.newBuilder(uri("/api/v1/shipments")).GET());

        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED);
        assertProblemShape(response, "AUTHENTICATION_REQUIRED");
    }

    @Test
    @DisplayName("Yanlış yöntem 405 ve proje biçiminde Türkçe hata döner")
    void shouldReturnProblemForUnsupportedMethod() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri(LOGIN)).GET());

        assertThat(response.statusCode()).isEqualTo(METHOD_NOT_ALLOWED);
        assertProblemShape(response, "METHOD_NOT_ALLOWED");
        assertThat(response.body()).contains("İşlem desteklenmiyor").doesNotContain("is not supported");
    }

    @Test
    @DisplayName("Desteklenmeyen içerik türü 415 ve proje biçiminde hata döner")
    void shouldReturnProblemForUnsupportedMediaType() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri(LOGIN))
                .header(CONTENT_TYPE, "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("admin")));

        assertThat(response.statusCode()).isEqualTo(UNSUPPORTED_MEDIA_TYPE);
        assertProblemShape(response, "UNSUPPORTED_MEDIA_TYPE");
    }

    @Test
    @DisplayName("Eksik alanlı giriş gövdesi 400 VALIDATION_FAILED döner; İngilizce varsayılan metin dönmez")
    void shouldReturnProblemForInvalidBody() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri(LOGIN))
                .header(CONTENT_TYPE, JSON)
                .POST(HttpRequest.BodyPublishers.ofString("{}")));

        assertThat(response.statusCode()).isEqualTo(BAD_REQUEST);
        assertProblemShape(response, "VALIDATION_FAILED");
        assertThat(response.body()).doesNotContain("Invalid request content");
    }

    private static void assertProblemShape(HttpResponse<String> response, String code) {
        JsonNode body = JsonMapper.shared().readTree(response.body());
        assertThat(response.headers().firstValue(CONTENT_TYPE).orElse("")).startsWith("application/problem+json");
        assertThat(body.path("code").asString()).isEqualTo(code);
        assertThat(body.path("status").asInt()).isEqualTo(response.statusCode());
        assertThat(body.path("type").asString()).startsWith("https://errors.yarnshipment.com/");
        assertThat(body.path("title").asString()).isNotBlank();
        assertThat(body.path("detail").asString()).isNotBlank();
        assertThat(body.path("instance").asString()).startsWith("/api/");
        assertThat(body.path("requestId").asString())
                .isEqualTo(response.headers().firstValue("X-Request-Id").orElseThrow());
    }

    private static String code(HttpResponse<String> response) {
        return JsonMapper.shared().readTree(response.body()).path("code").asString();
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws IOException, InterruptedException {
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
