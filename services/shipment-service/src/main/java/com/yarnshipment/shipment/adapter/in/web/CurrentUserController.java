package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.common.security.TokenClaims;
import com.yarnshipment.shipment.adapter.in.web.api.CurrentUserApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.CurrentUserResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.Permission;
import com.yarnshipment.shipment.adapter.in.web.api.model.Role;
import com.yarnshipment.shipment.adapter.in.web.api.model.WorkArea;
import com.yarnshipment.shipment.application.port.in.stock.GetWorkAreaUseCase;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

    private static final Set<String> CENTRAL_HIDDEN = Set.of("FABRIC_RECEIPT_RECORD");
    private static final Set<String> SUBCONTRACTOR_HIDDEN = Set.of("CENTRAL_STOCK_VIEW", "CENTRAL_PRODUCTION_RECORD");

    private final CurrentActorProvider currentActorProvider;
    private final GetWorkAreaUseCase workAreas;

    public CurrentUserController(CurrentActorProvider currentActorProvider, GetWorkAreaUseCase workAreas) {
        this.currentActorProvider = currentActorProvider;
        this.workAreas = workAreas;
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CurrentUserResponse> getCurrentUser() {
        Actor actor = currentActorProvider.getCurrentActor();
        Optional<WorkArea> area = workAreas.restrictedTo(actor).map(restricted -> WorkArea.valueOf(restricted.name()));
        CurrentUserResponse response = new CurrentUserResponse()
                .userId(actor.userId())
                .fullName(fullName(actor))
                .roles(actor.roles().stream()
                        .sorted()
                        .map(role -> Role.valueOf(role.name()))
                        .toList())
                .permissions(permissions(actor, area))
                .siteIds(List.copyOf(actor.siteIds()))
                .mfaVerified(actor.mfaVerified())
                .workArea(area.orElse(null));
        return ResponseEntity.ok(response);
    }

    /** Kalite kullanıcısına kendi çalışma alanı dışındaki izinler gösterilmez; menü ve formlar buna göre gizlenir. */
    private static List<Permission> permissions(Actor actor, Optional<WorkArea> area) {
        return actor.permissions().stream()
                .filter(permission -> !hiddenFor(area).contains(permission.name()))
                .sorted()
                .map(permission -> Permission.valueOf(permission.name()))
                .toList();
    }

    private static Set<String> hiddenFor(Optional<WorkArea> area) {
        return area.map(own -> own == WorkArea.CENTRAL ? CENTRAL_HIDDEN : SUBCONTRACTOR_HIDDEN)
                .orElse(Set.of());
    }

    private static String fullName(Actor actor) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String name = authentication instanceof JwtAuthenticationToken token
                ? token.getToken().getClaimAsString(TokenClaims.NAME)
                : null;
        return name == null ? actor.username() : name;
    }
}
