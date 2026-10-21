package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.common.security.TokenClaims;
import com.yarnshipment.shipment.adapter.in.web.api.CurrentUserApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.CurrentUserResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.Permission;
import com.yarnshipment.shipment.adapter.in.web.api.model.Role;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;

/**
 * Oturumdaki kullanıcı (yetkilendirme analizi, {@code /api/v1/me}). Frontend menü ve butonları buna göre gizler; asıl
 * kontrol uçlardadır. Faz 1'de yalnızca token'dan üretildiği için herhangi bir servis sunabilir; gateway bu uca
 * shipment-service'e yönlendirir.
 */
@RestController
public class CurrentUserController implements CurrentUserApi {

    private final CurrentActorProvider currentActorProvider;

    public CurrentUserController(CurrentActorProvider currentActorProvider) {
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CurrentUserResponse> getCurrentUser() {
        Actor actor = currentActorProvider.getCurrentActor();
        CurrentUserResponse response = new CurrentUserResponse()
                .userId(actor.userId())
                .fullName(fullName(actor))
                .roles(actor.roles().stream()
                        .sorted()
                        .map(role -> Role.valueOf(role.name()))
                        .toList())
                .permissions(permissions(actor))
                .siteIds(List.copyOf(actor.siteIds()))
                .subcontractorId(actor.subcontractorId())
                .driverId(actor.driverId())
                .mfaVerified(actor.mfaVerified());
        return ResponseEntity.ok(response);
    }

    /** COMPARISON_RUN yalnızca SYSTEM içindir ve sözleşmede yoktur. */
    private static List<Permission> permissions(Actor actor) {
        return actor.permissions().stream()
                .filter(permission -> permission != com.yarnshipment.common.security.Permission.COMPARISON_RUN)
                .sorted()
                .map(permission -> Permission.valueOf(permission.name()))
                .toList();
    }

    private static String fullName(Actor actor) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String name = authentication instanceof JwtAuthenticationToken token
                ? token.getToken().getClaimAsString(TokenClaims.NAME)
                : null;
        return name == null ? actor.username() : name;
    }
}
