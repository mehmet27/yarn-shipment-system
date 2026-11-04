package com.yarnshipment.shipment.masterdata.application.port.in;

import com.yarnshipment.common.security.Actor;

/**
 * Depo, kalite, müdür ya da yönetici hesabı açar (yalnızca yönetici). Şifre üretilmez ve gösterilmez; kişiye şifresini
 * belirleyeceği bir e-posta bağlantısı gider.
 */
public interface CreateStaffLoginAccountUseCase {

    void createStaffLoginAccount(Actor actor, StaffAccountRequest request);
}
