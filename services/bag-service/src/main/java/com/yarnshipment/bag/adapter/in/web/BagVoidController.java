package com.yarnshipment.bag.adapter.in.web;

import com.yarnshipment.bag.adapter.in.web.api.BagVoidApi;
import com.yarnshipment.bag.adapter.in.web.api.model.VoidBagsRequest;
import com.yarnshipment.bag.adapter.in.web.api.model.VoidBagsResponse;
import com.yarnshipment.bag.application.port.in.VoidBagsCommand;
import com.yarnshipment.bag.application.port.in.VoidBagsUseCase;
import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.CurrentActorProvider;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Çuval iptali; {@code BAG_CREATE} ister. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class BagVoidController implements BagVoidApi {

    private final VoidBagsUseCase voidBagsUseCase;
    private final CurrentActorProvider currentActorProvider;

    public BagVoidController(VoidBagsUseCase voidBagsUseCase, CurrentActorProvider currentActorProvider) {
        this.voidBagsUseCase = voidBagsUseCase;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('BAG_CREATE')")
    public ResponseEntity<VoidBagsResponse> voidBags(VoidBagsRequest request) {
        Actor actor = currentActorProvider.getCurrentActor();
        VoidBagsCommand command = new VoidBagsCommand(actor, List.copyOf(request.getUniqueKeys()), request.getReason());
        return ResponseEntity.ok(new VoidBagsResponse(voidBagsUseCase.voidBags(command)));
    }
}
