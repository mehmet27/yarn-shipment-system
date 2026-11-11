package com.yarnshipment.bag.adapter.in.web;

import com.yarnshipment.bag.adapter.in.web.api.BagsApi;
import com.yarnshipment.bag.adapter.in.web.api.model.BagView;
import com.yarnshipment.bag.application.port.in.GetBagQuery;
import com.yarnshipment.bag.application.port.in.GetBagUseCase;
import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.CurrentActorProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Çuval okuma ucu. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class BagController implements BagsApi {

    private final GetBagUseCase getBagUseCase;
    private final CurrentActorProvider currentActorProvider;

    public BagController(GetBagUseCase getBagUseCase, CurrentActorProvider currentActorProvider) {
        this.getBagUseCase = getBagUseCase;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('DISPATCH_RECORD')")
    public ResponseEntity<BagView> getBag(String uniqueKey) {
        Actor actor = currentActorProvider.getCurrentActor();
        return ResponseEntity.ok(BagViews.toView(getBagUseCase.getBag(new GetBagQuery(actor, uniqueKey))));
    }
}
