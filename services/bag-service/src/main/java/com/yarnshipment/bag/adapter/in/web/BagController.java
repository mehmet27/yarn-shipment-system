package com.yarnshipment.bag.adapter.in.web;

import com.yarnshipment.bag.adapter.in.web.api.BagsApi;
import com.yarnshipment.bag.adapter.in.web.api.model.BagView;
import com.yarnshipment.bag.adapter.in.web.api.model.ReleaseBagsRequest;
import com.yarnshipment.bag.adapter.in.web.api.model.ReleaseBagsResponse;
import com.yarnshipment.bag.adapter.in.web.api.model.ReserveBagsRequest;
import com.yarnshipment.bag.adapter.in.web.api.model.ReserveBagsResponse;
import com.yarnshipment.bag.application.port.in.GetBagQuery;
import com.yarnshipment.bag.application.port.in.GetBagUseCase;
import com.yarnshipment.bag.application.port.in.ReleaseBagsCommand;
import com.yarnshipment.bag.application.port.in.ReleaseBagsUseCase;
import com.yarnshipment.bag.application.port.in.ReserveBagsCommand;
import com.yarnshipment.bag.application.port.in.ReserveBagsUseCase;
import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.CurrentActorProvider;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Çuval uçları. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class BagController implements BagsApi {

    private final GetBagUseCase getBagUseCase;
    private final ReserveBagsUseCase reserveBagsUseCase;
    private final ReleaseBagsUseCase releaseBagsUseCase;
    private final CurrentActorProvider currentActorProvider;

    public BagController(
            GetBagUseCase getBagUseCase,
            ReserveBagsUseCase reserveBagsUseCase,
            ReleaseBagsUseCase releaseBagsUseCase,
            CurrentActorProvider currentActorProvider) {
        this.getBagUseCase = getBagUseCase;
        this.reserveBagsUseCase = reserveBagsUseCase;
        this.releaseBagsUseCase = releaseBagsUseCase;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('DISPATCH_RECORD')")
    public ResponseEntity<BagView> getBag(String uniqueKey) {
        Actor actor = currentActorProvider.getCurrentActor();
        return ResponseEntity.ok(BagViews.toView(getBagUseCase.getBag(new GetBagQuery(actor, uniqueKey))));
    }

    @Override
    @PreAuthorize("hasAuthority('DISPATCH_RECORD')")
    public ResponseEntity<ReserveBagsResponse> reserveBags(ReserveBagsRequest request) {
        Actor actor = currentActorProvider.getCurrentActor();
        ReserveBagsCommand command =
                new ReserveBagsCommand(actor, request.getShipmentId(), List.copyOf(request.getUniqueKeys()));
        return ResponseEntity.ok(new ReserveBagsResponse(reserveBagsUseCase.reserveBags(command).stream()
                .map(BagViews::toView)
                .toList()));
    }

    @Override
    @PreAuthorize("hasAuthority('SHIPMENT_CANCEL')")
    public ResponseEntity<ReleaseBagsResponse> releaseBags(ReleaseBagsRequest request) {
        Actor actor = currentActorProvider.getCurrentActor();
        ReleaseBagsCommand command = new ReleaseBagsCommand(actor, request.getShipmentId(), request.getReason());
        return ResponseEntity.ok(new ReleaseBagsResponse(releaseBagsUseCase.releaseBags(command)));
    }
}
