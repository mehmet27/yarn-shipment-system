package com.yarnshipment.shipment.masterdata.adapter.out.keycloak;

import com.yarnshipment.common.security.Role;
import com.yarnshipment.shipment.masterdata.application.port.out.LoginAccountPort;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSummary;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

/**
 * {@link LoginAccountPort}'un Keycloak Yönetim API'si uygulaması (giriş hesabı listesi). Liste, uygulama
 * rollerinin üyeleri birleştirilerek çıkarılır; böylece rolsüz servis hesapları listeye girmez ve kullanıcı başına
 * ek çağrı gerekmez.
 */
@Component
final class KeycloakLoginAccountAdapter implements LoginAccountPort {

    private static final int PAGE_SIZE = 100;

    private final KeycloakAdminClient admin;

    KeycloakLoginAccountAdapter(KeycloakAdminClient admin) {
        this.admin = admin;
    }

    @Override
    public List<LoginAccountSummary> findAll() {
        return admin.guarded(() -> {
            String token = admin.accessToken();
            Map<Role, List<Map<String, Object>>> membersByRole = new EnumMap<>(Role.class);
            for (Role role : KeycloakAccountSummaries.listedRoles()) {
                membersByRole.put(role, membersOf(token, role));
            }
            return KeycloakAccountSummaries.merge(membersByRole);
        });
    }

    /** Rolün doğrudan üyeleri, sayfa sayfa; realm'de tanımlı olmayan rol boş döner. */
    private List<Map<String, Object>> membersOf(String token, Role role) {
        String realmRole = role.name().toLowerCase(Locale.ROOT);
        List<Map<String, Object>> members = new ArrayList<>();
        List<Map<String, Object>> page;
        do {
            page = memberPage(token, realmRole, members.size());
            members.addAll(page);
        } while (page.size() == PAGE_SIZE);
        return members;
    }

    private List<Map<String, Object>> memberPage(String token, String realmRole, int first) {
        try {
            List<Map<String, Object>> page = admin.http()
                    .get()
                    .uri(uri -> uri.path("/admin/realms/{realm}/roles/{role}/users")
                            .queryParam("first", first)
                            .queryParam("max", PAGE_SIZE)
                            .queryParam("briefRepresentation", false)
                            .build(admin.realm(), realmRole))
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .body(KeycloakAdminClient.OBJECTS);
            return page == null ? List.of() : page;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return List.of();
            }
            throw exception;
        }
    }

}
