package com.yarnshipment.bag.application.service;

import com.yarnshipment.bag.application.port.in.ReleaseBagsCommand;
import com.yarnshipment.bag.application.port.in.ReleaseBagsUseCase;
import com.yarnshipment.bag.application.port.out.BagQueryPort;
import com.yarnshipment.bag.application.port.out.BagReservationPort;
import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.scope.ScopeGuard;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sevkiyatın rezervasyonlarını bırakır. Her çuvalın lokasyonu isteyenin kapsamında olmalıdır. */
@Service
public class ReleaseBagsService implements ReleaseBagsUseCase {

    private final BagQueryPort queryPort;
    private final BagReservationPort reservationPort;
    private final ScopeGuard scopeGuard;
    private final Clock clock;

    public ReleaseBagsService(
            BagQueryPort queryPort, BagReservationPort reservationPort, ScopeGuard scopeGuard, Clock clock) {
        this.queryPort = queryPort;
        this.reservationPort = reservationPort;
        this.scopeGuard = scopeGuard;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int releaseBags(ReleaseBagsCommand command) {
        List<Bag> reserved = queryPort.findReservedFor(command.shipmentId());
        BagScopes.validateAll(scopeGuard, command.actor(), reserved, Permission.SHIPMENT_CANCEL.name());
        return reservationPort.release(
                command.shipmentId(), command.reason(), command.actor().userId(), clock.instant());
    }
}
