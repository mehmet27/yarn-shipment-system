package com.yarnshipment.shipment.masterdata.adapter.out.keycloak;

import com.yarnshipment.shipment.masterdata.application.port.out.LoginAccountPort;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountExistsException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSpec;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountUnavailableException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginRole;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * {@link LoginAccountPort}'un Keycloak Yönetim API'si uygulaması. Servis hesabıyla ({@code client_credentials})
 * çalışır; bu hesabın yalnızca kullanıcı yönetimi ve rol okuma yetkisi olmalıdır. Kullanıcı açıldıktan sonra rol
 * atanamazsa kullanıcı silinir, yarım hesap kalmaz. Şifre ve token loglanmaz.
 */
@Component
final class KeycloakLoginAccountAdapter implements LoginAccountPort {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final String SITE_IDS = "site_ids";
    private static final ParameterizedTypeReference<Map<String, Object>> OBJECT = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Map<String, Object>>> OBJECTS =
            new ParameterizedTypeReference<>() {};

    private final RestClient client;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    KeycloakLoginAccountAdapter(
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

    @Override
    public void create(LoginAccountSpec spec) {
        if (clientSecret.isBlank()) {
            throw new LoginAccountUnavailableException("Keycloak admin client is not configured");
        }
        try {
            String token = accessToken();
            requireNoAccountFor(token, spec);
            Map<String, Object> role = realmRole(token, spec.role());
            String userId = createUser(token, spec);
            assignRole(token, userId, role);
        } catch (RestClientResponseException exception) {
            throw new LoginAccountUnavailableException(
                    "Keycloak rejected the call status=" + exception.getStatusCode(), exception);
        } catch (RestClientException exception) {
            throw new LoginAccountUnavailableException("Keycloak call failed", exception);
        }
    }

    private String accessToken() {
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

    private void requireNoAccountFor(String token, LoginAccountSpec spec) {
        List<Map<String, Object>> existing = client.get()
                .uri(uri -> uri.path("/admin/realms/{realm}/users")
                        .queryParam("q", spec.role().ownerAttribute() + ":" + spec.ownerId())
                        .build(realm))
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(OBJECTS);
        if (existing != null && !existing.isEmpty()) {
            throw new LoginAccountExistsException("record");
        }
    }

    private Map<String, Object> realmRole(String token, LoginRole role) {
        Map<String, Object> found = client.get()
                .uri("/admin/realms/{realm}/roles/{role}", realm, role.realmRole())
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(OBJECT);
        if (found == null) {
            throw new LoginAccountUnavailableException("Keycloak role not found role=" + role.realmRole());
        }
        return Map.of("id", found.get("id"), "name", found.get("name"));
    }

    private static Map<String, Object> userRepresentation(LoginAccountSpec spec) {
        String[] names = splitName(spec.displayName());
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("username", spec.username());
        user.put("enabled", true);
        user.put("firstName", names[0]);
        user.put("lastName", names[1]);
        if (spec.email() != null) {
            user.put("email", spec.email());
        }
        user.put(
                "attributes",
                Map.of(
                        spec.role().ownerAttribute(),
                        List.of(spec.ownerId().toString()),
                        SITE_IDS,
                        spec.siteIds().stream().map(Object::toString).toList()));
        user.put("requiredActions", List.of("UPDATE_PASSWORD"));
        user.put(
                "credentials",
                List.of(Map.of("type", "password", "value", spec.temporaryPassword(), "temporary", true)));
        return user;
    }

    private String createUser(String token, LoginAccountSpec spec) {
        Map<String, Object> user = userRepresentation(spec);
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
            throw exception;
        }
    }

    private void assignRole(String token, String userId, Map<String, Object> role) {
        try {
            client.post()
                    .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, userId)
                    .headers(headers -> headers.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(role))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            deleteQuietly(token, userId, exception);
            throw exception;
        }
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

    /** Kullanıcı profili ad ve soyad ister; tek parçalı adlarda soyad aynı değerle doldurulur. */
    private static String[] splitName(String displayName) {
        String name = displayName.strip();
        int split = name.lastIndexOf(' ');
        return split < 0
                ? new String[] {name, name}
                : new String[] {name.substring(0, split), name.substring(split + 1)};
    }
}
