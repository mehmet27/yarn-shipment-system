package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Role;
import com.yarnshipment.common.security.scope.OutOfScopeAccessException;
import com.yarnshipment.shipment.application.port.in.stock.WorkAreaUseCase;
import com.yarnshipment.shipment.application.port.out.ledger.WorkAreaPort;
import com.yarnshipment.shipment.domain.stock.WorkArea;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Kalite kullanıcısının çalışma alanını uygular; yalnız kalite rolündeki (yönetici olmayan) kullanıcı kısıtlıdır. */
@Service
public class WorkAreaService implements WorkAreaUseCase {

    private final WorkAreaPort workAreas;

    public WorkAreaService(WorkAreaPort workAreas) {
        this.workAreas = workAreas;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkArea> restrictedTo(Actor actor) {
        if (!actor.hasRole(Role.QUALITY) || actor.hasRole(Role.ADMIN)) {
            return Optional.empty();
        }
        return workAreas.findByUser(actor.userId());
    }

    @Override
    @Transactional(readOnly = true)
    public void requireAccess(Actor actor, WorkArea area) {
        restrictedTo(actor).filter(own -> own != area).ifPresent(own -> {
            throw new OutOfScopeAccessException("WorkArea:" + area);
        });
    }
}
