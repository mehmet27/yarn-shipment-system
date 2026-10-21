package com.yarnshipment.common.security.config;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.common.security.TestTokens;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.common.security.scope.ScopedResource;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Yetki ve kapsam kurallarını HTTP üzerinden denemek için uçlar. Method security proxy'si için final değildir. */
@RestController
@RequestMapping("/test/shipments")
class SecuredTestController {

    private final CurrentActorProvider currentActorProvider;
    private final ScopeGuard scopeGuard;

    SecuredTestController(CurrentActorProvider currentActorProvider, ScopeGuard scopeGuard) {
        this.currentActorProvider = currentActorProvider;
        this.scopeGuard = scopeGuard;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SHIPMENT_VIEW')")
    public String listShipments() {
        return currentActorProvider.getCurrentActor().roles().toString();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SHIPMENT_CREATE')")
    public void createShipment() {
        // Yalnızca yetki kontrolü denenir.
    }

    @GetMapping("/of-subcontractor/{subcontractorId}")
    @PreAuthorize("hasAuthority('SHIPMENT_VIEW')")
    public String getShipment(@PathVariable UUID subcontractorId) {
        ScopedResource shipment =
                new ScopedResource("Shipment", UUID.randomUUID(), TestTokens.SITE_ID, subcontractorId, null);
        scopeGuard.validateInScope(currentActorProvider.getCurrentActor(), shipment, "SHIPMENT_VIEW");
        return shipment.reference();
    }
}
