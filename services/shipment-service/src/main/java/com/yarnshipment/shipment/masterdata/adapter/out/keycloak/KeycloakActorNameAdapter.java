package com.yarnshipment.shipment.masterdata.adapter.out.keycloak;

import com.yarnshipment.shipment.application.port.out.ActorNamePort;
import com.yarnshipment.shipment.masterdata.application.port.out.LoginAccountPort;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSummary;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountUnavailableException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * {@link ActorNamePort}'un Keycloak uygulaması: uygulama rolü olan kullanıcıların ad soyadı (yoksa giriş adı)
 * kullanılır. Kimlik sağlayıcı yanıt vermezse geçmiş adsız döner; okuma hiç bozulmaz.
 */
@Component
final class KeycloakActorNameAdapter implements ActorNamePort {

    private final LoginAccountPort accounts;

    KeycloakActorNameAdapter(LoginAccountPort accounts) {
        this.accounts = accounts;
    }

    @Override
    public Map<UUID, String> namesOf(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Set<UUID> wanted = Set.copyOf(userIds);
        try {
            Map<UUID, String> names = new HashMap<>();
            for (LoginAccountSummary account : accounts.findAll()) {
                if (wanted.contains(account.id())) {
                    String fullName = account.fullName();
                    names.put(account.id(), fullName == null || fullName.isBlank() ? account.username() : fullName);
                }
            }
            return names;
        } catch (LoginAccountUnavailableException e) {
            return Map.of();
        }
    }
}
