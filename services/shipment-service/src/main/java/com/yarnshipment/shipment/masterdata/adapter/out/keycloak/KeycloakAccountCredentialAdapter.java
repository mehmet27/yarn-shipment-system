package com.yarnshipment.shipment.masterdata.adapter.out.keycloak;

import com.yarnshipment.shipment.masterdata.application.port.out.AccountCredentialPort;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.WeakPasswordException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

/**
 * {@link AccountCredentialPort}'un Keycloak uygulaması: geçici şifre belirler ve hesabı açıp kapatır. Şifre yalnızca Keycloak'a giden istekte bulunur; loglanmaz ve saklanmaz.
 */
@Component
final class KeycloakAccountCredentialAdapter implements AccountCredentialPort {

    private static final String UPDATE_PASSWORD = "UPDATE_PASSWORD";

    private final KeycloakAdminClient admin;

    KeycloakAccountCredentialAdapter(KeycloakAdminClient admin) {
        this.admin = admin;
    }

    @Override
    public void setTemporaryPassword(UUID userId, String password) {
        admin.guardedRun(() -> replacePassword(admin.accessToken(), userId, password));
    }

    @Override
    public void enable(UUID userId) {
        admin.guardedRun(() -> switchEnabled(admin.accessToken(), userId, true));
    }

    @Override
    public void disable(UUID userId) {
        admin.guardedRun(() -> switchEnabled(admin.accessToken(), userId, false));
    }

    private void replacePassword(String token, UUID userId, String password) {
        try {
            admin.http()
                    .put()
                    .uri("/admin/realms/{realm}/users/{id}/reset-password", admin.realm(), userId)
                    .headers(headers -> headers.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("type", "password", "value", password, "temporary", true))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
                throw new WeakPasswordException(exception);
            }
            throw exception;
        }
        requirePasswordChange(token, userId);
        endSessions(token, userId);
    }

    private void switchEnabled(String token, UUID userId, boolean enabled) {
        if (user(token, userId).isEmpty()) {
            throw new LoginAccountNotFoundException();
        }
        putUser(token, userId, Map.of("enabled", enabled));
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

    /** Geçici şifre verilen kişi ilk girişte şifresini değiştirir; zorunlu eylem yoksa eklenir. */
    private void requirePasswordChange(String token, UUID userId) {
        Map<String, Object> current = user(token, userId).orElseThrow(LoginAccountNotFoundException::new);
        List<Object> actions = new ArrayList<>();
        if (current.get("requiredActions") instanceof List<?> existing) {
            actions.addAll(existing);
        }
        if (!actions.contains(UPDATE_PASSWORD)) {
            actions.add(UPDATE_PASSWORD);
            putUser(token, userId, Map.of("requiredActions", actions));
        }
    }

    private void putUser(String token, UUID userId, Map<String, Object> changes) {
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
}
