package com.yarnshipment.shipment.masterdata.application.port.in.account;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
import com.yarnshipment.shipment.masterdata.domain.model.StaffWorkArea;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Fabrika içi hesabın rolünü değiştirir (yalnızca yönetici); kişi kendi rolünü değiştiremez. Kalite rolünde çalışma
 * alanı zorunludur ve aynı çağrıda değişebilir; başka rolde verilemez, kaydı silinir.
 */
public interface ChangeLoginAccountRoleUseCase {

    void changeLoginAccountRole(Actor actor, UUID userId, StaffRole role, @Nullable StaffWorkArea workArea);
}
