package com.yarnshipment.bag.application.service;

import com.yarnshipment.bag.application.port.in.BagNotFoundException;
import com.yarnshipment.bag.application.port.in.VoidBagsCommand;
import com.yarnshipment.bag.application.port.in.VoidBagsUseCase;
import com.yarnshipment.bag.application.port.out.BagQueryPort;
import com.yarnshipment.bag.application.port.out.BagVoidPort;
import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.bag.domain.model.BagNotVoidableException;
import com.yarnshipment.bag.domain.model.BagStatus;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.scope.ScopeGuard;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Yanlış üretilen çuvalı iptal eder. Yalnızca depodaki (CREATED) çuval iptal edilir; sevkiyata ayrılmış ya da yola
 * çıkmış çuval iptal edilemez. Zaten iptal edilmiş çuval sessizce atlanır, bu yüzden tekrar gönderim zarar vermez.
 */
@Service
public class VoidBagsService implements VoidBagsUseCase {

    private final BagQueryPort bags;
    private final BagVoidPort voids;
    private final ScopeGuard scopeGuard;
    private final Clock clock;

    public VoidBagsService(BagQueryPort bags, BagVoidPort voids, ScopeGuard scopeGuard, Clock clock) {
        this.bags = bags;
        this.voids = voids;
        this.scopeGuard = scopeGuard;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int voidBags(VoidBagsCommand command) {
        List<Bag> found = inRequestOrder(command.uniqueKeys(), bags.findByUniqueKeys(command.uniqueKeys()));
        BagScopes.validateAll(scopeGuard, command.actor(), found, Permission.BAG_CREATE.name());
        found.stream()
                .filter(bag -> bag.status() != BagStatus.CREATED && bag.status() != BagStatus.VOID)
                .findFirst()
                .ifPresent(bag -> {
                    throw new BagNotVoidableException(bag.uniqueKey(), bag.status());
                });
        List<Bag> toVoid =
                found.stream().filter(bag -> bag.status() == BagStatus.CREATED).toList();
        voids.voidBags(toVoid, command.reason().strip(), command.actor().userId(), clock.instant());
        return toVoid.size();
    }

    /** Bulunamayan anahtar varsa hiçbiri iptal edilmez. */
    private static List<Bag> inRequestOrder(List<String> uniqueKeys, List<Bag> found) {
        Map<String, Bag> byKey = found.stream().collect(Collectors.toMap(Bag::uniqueKey, Function.identity()));
        return uniqueKeys.stream()
                .map(key -> {
                    Bag bag = byKey.get(key);
                    if (bag == null) {
                        throw new BagNotFoundException(key);
                    }
                    return bag;
                })
                .toList();
    }
}
