package com.yarnshipment.shipment.masterdata.application.port.in.account;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
import java.util.UUID;

/** Fabrika içi hesabın rolünü değiştirir (yalnızca yönetici); kişi kendi rolünü değiştiremez. */
public interface ChangeLoginAccountRoleUseCase {

    void changeLoginAccountRole(Actor actor, UUID userId, StaffRole role);
}
