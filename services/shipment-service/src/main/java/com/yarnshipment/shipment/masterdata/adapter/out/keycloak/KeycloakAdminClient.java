package com.yarnshipment.shipment.masterdata.adapter.out.keycloak;

import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountExistsException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountUnavailableException;
import com.yarnshipment.shipment.masterdata.domain.model.WeakPasswordException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Keycloak Yönetim API'sine servis hesabıyla ({@code client_credentials}) bağlanan ortak istemci. Hesap adaptörleri
 * token, rol, kullanıcı açma ve geri alma adımlarını buradan kullanır. Şifre ve token loglanmaz.
 */
@Component
final class KeycloakAdminClient {

    static final ParameterizedTypeReference<Map<String, Object>> OBJECT = new ParameterizedTypeReference<>() {};
    static final ParameterizedTypeReference<List<Map<String, Object>>> OBJECTS = new ParameterizedTypeReference<>() {};

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final RestClient client;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    KeycloakAdminClient(
            @Value("${yarnshipment.keycloak-admin.url:http://localhost:8180}") String baseUrl,
            @Value("${yarnshipment.keycloak-admin.realm:yarnshipment}") String realm,
            @Value("${yarnshipment.keycloak-admin.client-id:yarnshipment-admin}") String clientId,
            @Value("${yarnshipment.keycloak-admin.client-secret:}") String clientSecret) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(http);
        requests.setReadTimeout(TIMEOUT);
        this.client =
                RestClient.builder().baseUrl(baseUrl).requestFactory(requests).build();
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    RestClient http() {
        return client;
    }

    String realm() {
        return realm;
    }

    /**
     * Sağlayıcı hatalarını {@link LoginAccountUnavailableException}'a çevirerek çağırır; yapılandırma yoksa hiç
     * çağırmaz.
     */
    <T> T guarded(Supplier<T> call) {
        if (clientSecret.isBlank()) {
            throw new LoginAccountUnavailableException("Keycloak admin client is not configured");
        }
        try {
            return call.get();
        } catch (RestClientResponseException exception) {
            throw new LoginAccountUnavailableException(
                    "Keycloak rejected the call status=" + exception.getStatusCode(), exception);
        } catch (RestClientException exception) {
            throw new LoginAccountUnavailableException("Keycloak call failed", exception);
        }
    }

    /** Dönüş değeri olmayan işlemler için {@link #guarded(Supplier)}. */
    void guardedRun(Runnable call) {
        guarded(() -> {
            call.run();
            return Boolean.TRUE;
        });
    }

    String accessToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        Map<String, Object> response = client.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(OBJECT);
        Object token = response == null ? null : response.get("access_token");
        if (!(token instanceof String value)) {
            throw new LoginAccountUnavailableException("Keycloak returned no access token");
        }
        return value;
    }

    Map<String, Object> realmRole(String token, String roleName) {
        Map<String, Object> found = client.get()
                .uri("/admin/realms/{realm}/roles/{role}", realm, roleName)
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(OBJECT);
        if (found == null) {
            throw new LoginAccountUnavailableException("Keycloak role not found role=" + roleName);
        }
        return Map.of("id", found.get("id"), "name", found.get("name"));
    }

    /**
     * Kullanıcıyı açar ve kimliğini döner; giriş adı alınmışsa {@link LoginAccountExistsException}, şifre kurala
     * uymuyorsa {@link WeakPasswordException}.
     */
    String createUser(String token, Map<String, Object> user) {
        try {
            URI location = client.post()
                    .uri("/admin/realms/{realm}/users", realm)
                    .headers(headers -> headers.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(user)
                    .retrieve()
                    .toBodilessEntity()
                    .getHeaders()
                    .getLocation();
            return idOf(location);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
                throw new LoginAccountExistsException("username");
            }
            if (exception.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
                throw new WeakPasswordException(exception);
            }
            throw exception;
        }
    }

    void assignRole(String token, String userId, Map<String, Object> role) {
        client.post()
                .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, userId)
                .headers(headers -> headers.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity();
    }

    /** Adım başarısız olursa yeni açılan kullanıcı silinir; yarım hesap kalmaz. */
    void rollbackOnFailure(String token, String userId, Runnable step) {
        try {
            step.run();
        } catch (RestClientException exception) {
            deleteQuietly(token, userId, exception);
            throw exception;
        }
    }

    /** Kullanıcı profili ad ve soyad ister; tek parçalı adlarda soyad aynı değerle doldurulur. */
    static String[] splitName(String displayName) {
        String name = displayName.strip();
        int split = name.lastIndexOf(' ');
        return split < 0
                ? new String[] {name, name}
                : new String[] {name.substring(0, split), name.substring(split + 1)};
    }

    private void deleteQuietly(String token, String userId, RestClientException cause) {
        try {
            client.delete()
                    .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException cleanup) {
            cause.addSuppressed(cleanup);
        }
    }

    private static String idOf(@Nullable URI location) {
        if (location == null) {
            throw new LoginAccountUnavailableException("Keycloak returned no user location");
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
