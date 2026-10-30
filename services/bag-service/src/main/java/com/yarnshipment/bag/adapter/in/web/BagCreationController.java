package com.yarnshipment.bag.adapter.in.web;

import com.yarnshipment.bag.adapter.in.web.api.BagCreationApi;
import com.yarnshipment.bag.adapter.in.web.api.model.CreateBagsRequest;
import com.yarnshipment.bag.adapter.in.web.api.model.CreateBagsResponse;
import com.yarnshipment.bag.adapter.in.web.api.model.LockedLotsResponse;
import com.yarnshipment.bag.adapter.in.web.api.model.LotBagsResponse;
import com.yarnshipment.bag.application.port.in.CreateBagsCommand;
import com.yarnshipment.bag.application.port.in.CreateBagsUseCase;
import com.yarnshipment.bag.application.port.in.ListLockedLotsQuery;
import com.yarnshipment.bag.application.port.in.ListLockedLotsUseCase;
import com.yarnshipment.bag.application.port.in.ListLotBagsQuery;
import com.yarnshipment.bag.application.port.in.ListLotBagsUseCase;
import com.yarnshipment.bag.domain.model.PackageType;
import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.CurrentActorProvider;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Çuval (Unique Key) üretimi ve partilerin çuval listesi; {@code BAG_CREATE} ister (depo ve admin). Method
 * security proxy'si gerektirdiği için final değildir (SEC-02).
 */
@RestController
public class BagCreationController implements BagCreationApi {

    private final CreateBagsUseCase createBagsUseCase;
    private final ListLockedLotsUseCase listLockedLotsUseCase;
    private final ListLotBagsUseCase listLotBagsUseCase;
    private final CurrentActorProvider currentActorProvider;

    public BagCreationController(
            CreateBagsUseCase createBagsUseCase,
            ListLockedLotsUseCase listLockedLotsUseCase,
            ListLotBagsUseCase listLotBagsUseCase,
            CurrentActorProvider currentActorProvider) {
        this.createBagsUseCase = createBagsUseCase;
        this.listLockedLotsUseCase = listLockedLotsUseCase;
        this.listLotBagsUseCase = listLotBagsUseCase;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('BAG_CREATE')")
    public ResponseEntity<CreateBagsResponse> createBags(CreateBagsRequest request) {
        Actor actor = currentActorProvider.getCurrentActor();
        CreateBagsCommand command = new CreateBagsCommand(
                actor,
                request.getLotId(),
                PackageType.valueOf(request.getPackageType().name()),
                request.getCount());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateBagsResponse(createBagsUseCase.createBags(command).stream()
                        .map(BagViews::toView)
                        .toList()));
    }

    @Override
    @PreAuthorize("hasAuthority('BAG_CREATE')")
    public ResponseEntity<LockedLotsResponse> listLockedLots() {
        Actor actor = currentActorProvider.getCurrentActor();
        return ResponseEntity.ok(new LockedLotsResponse(
                listLockedLotsUseCase.listLockedLots(new ListLockedLotsQuery(actor)).stream()
                        .map(BagViews::toView)
                        .toList()));
    }

    @Override
    @PreAuthorize("hasAuthority('BAG_CREATE')")
    public ResponseEntity<LotBagsResponse> listLotBags(UUID lotId) {
        Actor actor = currentActorProvider.getCurrentActor();
        return ResponseEntity.ok(new LotBagsResponse(
                listLotBagsUseCase.listLotBags(new ListLotBagsQuery(actor, lotId)).stream()
                        .map(BagViews::toView)
                        .toList()));
    }
}
