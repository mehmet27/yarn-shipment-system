package com.yarnshipment.shipment.masterdata.adapter.out.keycloak;

import com.yarnshipment.shipment.masterdata.application.port.out.AccountCredentialPort;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.ResetTarget;
import com.yarnshipment.shipment.masterdata.domain.model.WeakPasswordException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

/**
 * {@link AccountCredentialPort}'un Keycloak uygulaması: kullanıcıyı giriş adı ya da kimliğiyle bulur, kalıcı şifre
 * belirler ve hesabı açıp kapatır. Şifre yalnızca Keycloak'a giden istekte bulunur; loglanmaz ve saklanmaz.
 */
@Component
final class KeycloakAccountCredentialAdapter implements AccountCredentialPort {

    private static final String UPDATE_PASSWORD = "UPDATE_PASSWORD";

    private final KeycloakAdminClient admin;

    KeycloakAccountCredentialAdapter(KeycloakAdminClient admin) {
        this.admin = admin;
    }

    @Override
    public Optional<ResetTarget> findByUsername(String username) {
        return admin.guarded(() -> lookUp(admin.accessToken(), username));
    }

    @Override
    public Optional<ResetTarget> findById(UUID userId) {
        return admin.guarded(() -> user(admin.accessToken(), userId).flatMap(KeycloakAccountCredentialAdapter::target));
    }

    @Override
    public void setPassword(UUID userId, String password) {
        admin.guardedRun(() -> replacePassword(admin.accessToken(), userId, password));
    }

    @Override
    public void setEnabled(UUID userId, boolean enabled) {
        admin.guardedRun(() -> switchEnabled(admin.accessToken(), userId, enabled));
    }

    private Optional<ResetTarget> lookUp(String token, String username) {
        List<Map<String, Object>> found = admin.http()
                .get()
                .uri(uri -> uri.path("/admin/realms/{realm}/users")
                        .queryParam("username", username)
                        .queryParam("exact", true)
                        .build(admin.realm()))
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(KeycloakAdminClient.OBJECTS);
        return found == null || found.isEmpty() ? Optional.empty() : target(found.getFirst());
    }

    private void replacePassword(String token, UUID userId, String password) {
        try {
            admin.http()
                    .put()
                    .uri("/admin/realms/{realm}/users/{id}/reset-password", admin.realm(), userId)
                    .headers(headers -> headers.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("type", "password", "value", password, "temporary", false))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
                throw new WeakPasswordException(exception);
            }
            throw exception;
        }
        clearPasswordChangeRequirement(token, userId);
        endSessions(token, userId);
    }

    private void switchEnabled(String token, UUID userId, boolean enabled) {
        if (user(token, userId).isEmpty()) {
            throw new LoginAccountNotFoundException();
        }
        update(token, userId, Map.of("enabled", enabled));
        if (!enabled) {
            endSessions(token, userId);
        }
    }

    private Optional<Map<String, Object>> user(String token, UUID userId) {
        try {
            return Optional.ofNullable(admin.http()
                    .get()
                    .uri("/admin/realms/{realm}/users/{id}", admin.realm(), userId)
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .body(KeycloakAdminClient.OBJECT));
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    /** Şifreyi kişi kendisi belirlediği için "şifre değiştir" zorunluluğu kalkar; diğer zorunlu eylemler kalır. */
    private void clearPasswordChangeRequirement(String token, UUID userId) {
        Map<String, Object> current = user(token, userId).orElseThrow(LoginAccountNotFoundException::new);
        if (current.get("requiredActions") instanceof List<?> actions && actions.contains(UPDATE_PASSWORD)) {
            List<?> remaining = actions.stream()
                    .filter(action -> !UPDATE_PASSWORD.equals(action))
                    .toList();
            update(token, userId, Map.of("requiredActions", remaining));
        }
    }

    private void update(String token, UUID userId, Map<String, Object> changes) {
        admin.http()
                .put()
                .uri("/admin/realms/{realm}/users/{id}", admin.realm(), userId)
                .headers(headers -> headers.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(changes)
                .retrieve()
                .toBodilessEntity();
    }

    private void endSessions(String token, UUID userId) {
        admin.http()
                .post()
                .uri("/admin/realms/{realm}/users/{id}/logout", admin.realm(), userId)
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .toBodilessEntity();
    }

    private static Optional<ResetTarget> target(Map<String, Object> user) {
        if (!(user.get("id") instanceof String id) || !(user.get("username") instanceof String username)) {
            return Optional.empty();
        }
        return Optional.of(new ResetTarget(
                UUID.fromString(id),
                username,
                Boolean.TRUE.equals(user.get("enabled")),
                KeycloakAccountSummaries.phone(user)));
    }
}
