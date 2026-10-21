package com.yarnshipment.common.security;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

/**
 * Keycloak erişim token'ını {@link ActorAuthenticationToken}'a çevirir (yetkilendirme analizi, "MFA ve oturum"):
 *
 * <ul>
 *   <li>Roller {@code roles} claim'inden okunur.
 *   <li>MFA zorunlu bir rol (varsayılan ADMIN, QUALITY) MFA'sız oturumda düşürülür.
 *   <li>Dış rol (DRIVER, SUBCONTRACTOR) fabrika içi rolle birleşmişse token reddedilir (görev ayrılığı, 401).
 *   <li>SUBCONTRACTOR için {@code subcontractor_id}, DRIVER için {@code driver_id} claim'i zorunludur.
 * </ul>
 */
public final class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger log = LoggerFactory.getLogger(KeycloakJwtConverter.class);

    private final SecurityProperties properties;

    public KeycloakJwtConverter(SecurityProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Actor actor = toActor(jwt);
        List<GrantedAuthority> authorities = new ArrayList<>(actor.permissions());
        actor.roles().forEach(role -> authorities.add(new SimpleGrantedAuthority(role.authority())));
        return new ActorAuthenticationToken(jwt, authorities, actor);
    }

    /** Token'ı doğrulanmış aktöre çevirir; kural ihlalinde {@link InvalidBearerTokenException} (401) fırlatır. */
    public Actor toActor(Jwt jwt) {
        boolean mfaVerified = isMfaVerified(jwt);
        Set<Role> roles = readRoles(jwt);
        if (!mfaVerified) {
            dropMfaRequiredRoles(jwt, roles);
        }
        validateSeparationOfDuties(roles);
        UUID subcontractorId = scopeClaim(jwt, roles, Role.SUBCONTRACTOR, TokenClaims.SUBCONTRACTOR_ID);
        UUID driverId = scopeClaim(jwt, roles, Role.DRIVER, TokenClaims.DRIVER_ID);
        String username = jwt.getClaimAsString(TokenClaims.PREFERRED_USERNAME);
        return new Actor(
                parseUuid(jwt.getSubject(), "sub"),
                username == null ? jwt.getSubject() : username,
                roles,
                RolePermissionMatrix.permissionsOf(roles),
                subcontractorId,
                driverId,
                readSiteIds(jwt),
                mfaVerified);
    }

    private Set<Role> readRoles(Jwt jwt) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        List<String> claimValues = jwt.getClaimAsStringList(TokenClaims.ROLES);
        if (claimValues != null) {
            claimValues.forEach(value -> Role.fromClaim(value).ifPresent(roles::add));
        }
        return roles;
    }

    private void dropMfaRequiredRoles(Jwt jwt, Set<Role> roles) {
        for (Role role : properties.mfaRequiredRoles()) {
            if (roles.remove(role)) {
                log.warn("Role ignored without MFA userId={} role={}", jwt.getSubject(), role);
            }
        }
    }

    private static void validateSeparationOfDuties(Set<Role> roles) {
        boolean hasExternal = roles.stream().anyMatch(Role::isExternal);
        boolean hasInternal = roles.stream().anyMatch(role -> !role.isExternal());
        if (hasExternal && hasInternal) {
            throw new InvalidBearerTokenException("External and internal roles cannot be combined");
        }
        if (roles.contains(Role.DRIVER) && roles.contains(Role.SUBCONTRACTOR)) {
            throw new InvalidBearerTokenException("Driver and subcontractor roles cannot be combined");
        }
    }

    private static @Nullable UUID scopeClaim(Jwt jwt, Set<Role> roles, Role role, String claim) {
        if (!roles.contains(role)) {
            return null;
        }
        String value = jwt.getClaimAsString(claim);
        if (value == null || value.isBlank()) {
            throw new InvalidBearerTokenException("Scope claim is required claim=" + claim);
        }
        return parseUuid(value, claim);
    }

    private static Set<UUID> readSiteIds(Jwt jwt) {
        List<String> values = jwt.getClaimAsStringList(TokenClaims.SITE_IDS);
        if (values == null) {
            return Set.of();
        }
        return Set.copyOf(values.stream()
                .map(value -> parseUuid(value, TokenClaims.SITE_IDS))
                .toList());
    }

    private boolean isMfaVerified(Jwt jwt) {
        List<String> amr = jwt.getClaimAsStringList(TokenClaims.AMR);
        if (amr != null && amr.stream().anyMatch(properties.mfaAmrValues()::contains)) {
            return true;
        }
        String acr = jwt.getClaimAsString(TokenClaims.ACR);
        return acr != null && properties.mfaAcrValues().contains(acr);
    }

    private static UUID parseUuid(@Nullable String value, String claim) {
        if (value == null) {
            throw new InvalidBearerTokenException("Claim is missing claim=" + claim);
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidBearerTokenException("Claim is not a UUID claim=" + claim, exception);
        }
    }
}
