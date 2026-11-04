package com.yarnshipment.shipment.masterdata.adapter.out.keycloak;

import com.yarnshipment.common.security.Role;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSummary;
import com.yarnshipment.shipment.masterdata.domain.model.PhoneNumber;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Keycloak kullanıcı gösterimlerini giriş hesabı özetine çevirir. Gösterimdeki kimlik bilgileri ({@code credentials})
 * hiç okunmaz; yalnızca liste için gereken alanlar alınır.
 */
final class KeycloakAccountSummaries {

    private static final String UPDATE_PASSWORD = "UPDATE_PASSWORD";

    private KeycloakAccountSummaries() {}

    /** SYSTEM yalnızca servis hesaplarındadır; kişi hesabı değildir. */
    static List<Role> listedRoles() {
        return Stream.of(Role.values()).filter(role -> role != Role.SYSTEM).toList();
    }

    /** Birden fazla rolün üyesi olan kullanıcı tek satırda birleşir; sonuç giriş adına göre sıralıdır. */
    static List<LoginAccountSummary> merge(Map<Role, List<Map<String, Object>>> membersByRole) {
        Map<String, Map<String, Object>> users = new LinkedHashMap<>();
        Map<String, List<String>> rolesByUser = new LinkedHashMap<>();
        membersByRole.forEach((role, members) -> {
            for (Map<String, Object> user : members) {
                if (user.get("id") instanceof String id) {
                    users.putIfAbsent(id, user);
                    rolesByUser.computeIfAbsent(id, key -> new ArrayList<>()).add(role.name());
                }
            }
        });
        return users.entrySet().stream()
                .map(entry ->
                        summary(entry.getKey(), entry.getValue(), rolesByUser.getOrDefault(entry.getKey(), List.of())))
                .sorted(Comparator.comparing(LoginAccountSummary::username))
                .toList();
    }

    private static LoginAccountSummary summary(String id, Map<String, Object> user, List<String> roles) {
        String username = user.get("username") instanceof String value ? value : id;
        boolean passwordChangeRequired =
                user.get("requiredActions") instanceof List<?> actions && actions.contains(UPDATE_PASSWORD);
        Instant createdAt =
                user.get("createdTimestamp") instanceof Number millis ? Instant.ofEpochMilli(millis.longValue()) : null;
        return new LoginAccountSummary(
                id,
                username,
                fullName(user),
                text(user.get("email")),
                maskedPhone(user),
                roles,
                Boolean.TRUE.equals(user.get("enabled")),
                passwordChangeRequired,
                createdAt);
    }

    /** Telefon özniteliği; okunamayan değer listeyi bozmaz, yalnızca telefonsuz sayılır. */
    static @Nullable PhoneNumber phone(Map<String, Object> user) {
        if (user.get("attributes") instanceof Map<?, ?> attributes
                && attributes.get(KeycloakAdminClient.PHONE_ATTRIBUTE) instanceof List<?> values
                && !values.isEmpty()
                && values.getFirst() instanceof String raw) {
            try {
                return PhoneNumber.parse(raw);
            } catch (InvalidMasterDataException unreadable) {
                return null;
            }
        }
        return null;
    }

    private static @Nullable String maskedPhone(Map<String, Object> user) {
        PhoneNumber phone = phone(user);
        return phone == null ? null : phone.masked();
    }

    /** Hesap açılırken tek parçalı adlarda soyad da aynı değerle doldurulur; listede bir kez gösterilir. */
    private static @Nullable String fullName(Map<String, Object> user) {
        String firstName = text(user.get("firstName"));
        String lastName = text(user.get("lastName"));
        if (firstName != null && firstName.equals(lastName)) {
            return firstName;
        }
        String joined = Stream.of(firstName, lastName).filter(Objects::nonNull).collect(Collectors.joining(" "));
        return joined.isEmpty() ? null : joined;
    }

    private static @Nullable String text(@Nullable Object value) {
        return value instanceof String text && !text.isBlank() ? text.strip() : null;
    }
}
