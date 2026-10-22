package com.yarnshipment.bag.application.service;

import com.yarnshipment.bag.application.port.in.BagNotFoundException;
import com.yarnshipment.bag.application.port.in.ReserveBagsCommand;
import com.yarnshipment.bag.application.port.in.ReserveBagsUseCase;
import com.yarnshipment.bag.application.port.out.BagQueryPort;
import com.yarnshipment.bag.application.port.out.BagReservationPort;
import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.scope.ScopeGuard;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Çuvalları sevkiyata ayırır. Her çuvalın lokasyonu isteyenin kapsamında olmalıdır. */
@Service
public class ReserveBagsService implements ReserveBagsUseCase {

    private final BagQueryPort queryPort;
    private final BagReservationPort reservationPort;
    private final ScopeGuard scopeGuard;
    private final Clock clock;

    public ReserveBagsService(
            BagQueryPort queryPort, BagReservationPort reservationPort, ScopeGuard scopeGuard, Clock clock) {
        this.queryPort = queryPort;
        this.reservationPort = reservationPort;
        this.scopeGuard = scopeGuard;
        this.clock = clock;
    }

    @Override
    @Transactional
    public List<Bag> reserveBags(ReserveBagsCommand command) {
        List<Bag> bags = queryPort.findByUniqueKeys(command.uniqueKeys());
        Set<String> found = new HashSet<>();
        bags.forEach(bag -> found.add(bag.uniqueKey()));
        command.uniqueKeys().stream()
                .filter(key -> !found.contains(key))
                .findFirst()
                .ifPresent(key -> {
                    throw new BagNotFoundException(key);
                });
        BagScopes.validateAll(scopeGuard, command.actor(), bags, Permission.DISPATCH_RECORD.name());
        reservationPort.reserve(command.shipmentId(), bags, command.actor().userId(), clock.instant());
        return queryPort.findByUniqueKeys(command.uniqueKeys());
    }
}
